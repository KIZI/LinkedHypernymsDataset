package cz.vse.lhd.hypernymextractor

import java.io._
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.{XPathConstants, XPathFactory}

import cz.vse.lhd.core.BasicFunction._
import cz.vse.lhd.core.lucene.NTIndexer
import cz.vse.lhd.core.{AnyToInt, AppConf, NTReader}
import cz.vse.lhd.hypernymextractor.builder.{DBpediaLinker, LocalResourceCache, MemCachedResourceCache, ResourceCache}
import gate.creole.SerialController
import gate.{Factory, FeatureMap, Gate, ProcessingResource}
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import org.w3c.dom.NodeList
import org.xml.sax.InputSource

import scala.io.Source
import scala.sys.process.Process

object RunDefaultPipeline extends AppConf {

  val logger = LoggerFactory.getLogger(getClass)

  tryClose((Conf.memcachedAddress, Conf.memcachedPort) match {
    case (Some(mAddress), Some(AnyToInt(mPort))) => new DBpediaLinker(Conf.wikiApi, Conf.lang) with MemCachedResourceCache {
      val address: String = mAddress
      val port: Int = mPort
    }
    case _ => new DBpediaLinker(Conf.wikiApi, Conf.lang) with LocalResourceCache
  }) { dbpediaLinker =>
    AppConf.args match {
      case Array(_, AnyToInt(offset), AnyToInt(limit)) =>
        logger.info("Number of resources for processing: " + limit)
        extractExactPart(offset, limit, dbpediaLinker)
      case Array(_, "index") =>
        indexDisambiguations()
      case _ =>
        extractBatch(dbpediaLinker)
    }
  }

  def extractBatch(cache: ResourceCache) = {
    cache.flush()
    logger.info("Number of resources for processing: " + Conf.datasetSize)
    val mvn = "\"C:\\Program Files (x86)\\JetBrains\\IntelliJ IDEA 14.1.1\\plugins\\maven\\lib\\maven3\\bin\\mvn.bat\""
    for (start <- (0 until Conf.datasetSize by Conf.corpusSizePerThread).par) retry(10) {
      val command = s"""-q scala:run -Dlauncher=runner -DaddArgs="${Conf.globalPropertiesFile}|$start|${Conf.corpusSizePerThread}" """.trim
      logger.info(s"Start command: $mvn $command")
      val exitCode = Process(s"$mvn $command").!
      logger.info(s"$mvn $command: has been finished with code: $exitCode")
      if (exitCode == 1) {
        logger.warn(s"Error of command: $mvn $command ; now try to process it again")
        throw new Exception(s"Error of command: $mvn $command (with exit code $exitCode)")
      }
    }
    val outputFiles = new File(Conf.outputDir).listFiles
    outputFiles
      .filter(_.getName.matches( """hypoutput\.\d+-\d+.*"""))
      .groupBy(_.getName.replaceAll( """.+\.""", ""))
      .foreach { case (fileType, files) =>
      tryClose(new BufferedOutputStream(new FileOutputStream(Conf.outputDir + s"hypoutput.log.$fileType"))) { outputStream =>
        for (file <- files) {
          tryClose(new BufferedInputStream(new FileInputStream(file))) { inputStream =>
            Stream continually inputStream.read takeWhile (_ != -1) foreach outputStream.write
          }
          file.delete()
        }
      }
    }
    outputFiles
      .filter(_.getName.matches( """.*\.completed$"""))
      .foreach(_.delete())
  }

  def extractExactPart(offset: Int, limit: Int, dbpediaLinker: DBpediaLinker) = {
    val gateHomeFile = new File(Conf.gateDir)
    val pluginsHomeFile = new File(Conf.gateDir.replaceAll("/+$", "") + "/plugins")
    Gate.setGateHome(gateHomeFile)
    Gate.setPluginsHome(pluginsHomeFile)
    Gate.init()
    Gate.getCreoleRegister.registerDirectories(new File(pluginsHomeFile, "ANNIE").toURI.toURL)
    Gate.getCreoleRegister.registerDirectories(new File(Conf.gatePluginLhdDir).toURI.toURL)
    Gate.getCreoleRegister.registerDirectories(new File(Gate.getPluginsHome, "Tagger_Framework").toURI.toURL)
    extractHypernyms(newFeatureMap(offset, limit, dbpediaLinker))
  }

  def indexDisambiguations(): Unit = tryCloses[Unit, AnyRef {def close(): Unit}](Source.fromFile(Conf.datasetDisambiguations, "UTF-8"), new NTIndexer(Conf.indexDir)) {
    case Seq(source: Source, indexer: NTIndexer) =>
      FileUtils.cleanDirectory(new File(Conf.indexDir))
      val disambiguations = NTReader.fromIterator(source.getLines()).map(_.getSubject.getURI).toSet
      logger.info(s"Disambiguations dataset has been loaded with size ${disambiguations.size}")
      logger.info("Now the disambiguations dataset is being indexed...")
      indexer.index(disambiguations.toIterator.map(subject => NTIndexer.Triple(subject, "", "")))
      logger.info("The disambiguations dataset has been indexed")
  }

  def extractHypernyms(featureMap: FeatureMap) = {
    val wikiPR = Factory.createResource("cz.vse.lhd.hypernymextractor.builder.CorpusBuilderPR2", featureMap).asInstanceOf[ProcessingResource]
    val cPipeline = Factory.createResource("gate.creole.SerialController").asInstanceOf[SerialController]
    cPipeline.add(wikiPR)
    cPipeline.execute()
  }

  def newFeatureMap(offset: Int, limit: Int, dbpediaLinker: DBpediaLinker) = {
    val list = XPathFactory.newInstance.newXPath.compile("//PARAMETER").evaluate(
      DocumentBuilderFactory.newInstance.newDocumentBuilder.parse(new InputSource(
        new BufferedReader(new InputStreamReader(
          new DataInputStream(new FileInputStream(Conf.gatePluginLhdDir + "/creole.xml")))))),
      XPathConstants.NODESET).asInstanceOf[NodeList]
    val featureMap = Factory.newFeatureMap
    for (i <- 0 until list.getLength) {
      val paramName = list.item(i).getAttributes.getNamedItem("NAME").getTextContent
      val paramVal = list.item(i).getAttributes.getNamedItem("DEFAULT").getTextContent
      featureMap.put(paramName, paramVal)
    }
    featureMap.put("disambiguations", new NTIndexer(Conf.indexDir))
    featureMap.put("dbpediaLinker", dbpediaLinker)
    featureMap.put("startPosInArticleNameList", "" + offset)
    featureMap.put("endPosInArticleNameList", "" + (offset + limit))
    featureMap
  }

}
