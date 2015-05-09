package cz.vse.lhd.hypernymextractor

import java.io._
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.{XPathConstants, XPathFactory}

import cz.vse.lhd.core.BasicFunction._
import cz.vse.lhd.core.{AnyToInt, AppConf, NTReader}
import cz.vse.lhd.hypernymextractor.builder.{LocalResourceCache, DBpediaLinker, MemCachedResourceCache}
import gate.creole.SerialController
import gate.{Factory, FeatureMap, Gate, ProcessingResource}
import org.slf4j.LoggerFactory
import org.w3c.dom.NodeList
import org.xml.sax.InputSource

import scala.io.Source

object RunDefaultPipeline extends AppConf {

  private val monitor = new AnyRef

  val logger = LoggerFactory.getLogger(getClass)
  val gateHomeFile = new File(Conf.gateDir)
  val pluginsHomeFile = new File(Conf.gateDir.replaceAll("/+$", "") + "/plugins")

  Gate.setGateHome(gateHomeFile)
  Gate.setPluginsHome(pluginsHomeFile)
  Gate.init()
  Gate.getCreoleRegister.registerDirectories(new File(pluginsHomeFile, "ANNIE").toURI.toURL)
  Gate.getCreoleRegister.registerDirectories(new File(Conf.gatePluginLhdDir).toURI.toURL)
  Gate.getCreoleRegister.registerDirectories(new File(Gate.getPluginsHome, "Tagger_Framework").toURI.toURL)
  logger.info("Gate initialized.")

  logger.info("Cache has been connected successfully.")
  val disambiguations = tryClose(Source.fromFile(Conf.datasetDisambiguations, "UTF-8")) {
    source =>
      NTReader.fromIterator(source.getLines()).map(_.getSubject.getURI).toSet
  }
  logger.info(s"Disambiguations dataset has been loaded with size ${disambiguations.size}")

  val list = XPathFactory.newInstance.newXPath.compile("//PARAMETER").evaluate(
    DocumentBuilderFactory.newInstance.newDocumentBuilder.parse(new InputSource(
      new BufferedReader(new InputStreamReader(
        new DataInputStream(new FileInputStream(Conf.gatePluginLhdDir + "/creole.xml")))))),
    XPathConstants.NODESET).asInstanceOf[NodeList]

  tryClose((Conf.memcachedAddress, Conf.memcachedPort) match {
    case (Some(mAddress), Some(AnyToInt(mPort))) => new DBpediaLinker(Conf.wikiApi, Conf.lang) with MemCachedResourceCache {
      val address: String = mAddress
      val port: Int = mPort
    }
    case _ => new DBpediaLinker(Conf.wikiApi, Conf.lang) with LocalResourceCache
  }) { dbpediaLinker =>
    dbpediaLinker.flush()
    AppConf.args match {
      case Array(_, AnyToInt(offset), AnyToInt(limit)) =>
        logger.info("Number of resources for processing: " + limit)
        extractHypernyms(newFeatureMap(offset, limit, dbpediaLinker))
      case _ =>
        logger.info("Number of resources for processing: " + Conf.datasetSize)
        for (start <- (0 until Conf.datasetSize by Conf.corpusSizePerThread).par) {
          extractHypernyms(newFeatureMap(start, Conf.corpusSizePerThread, dbpediaLinker))
        }
    }
  }

  new File(Conf.outputDir)
    .listFiles
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

  def extractHypernyms(featureMap: FeatureMap) = monitor.synchronized {
    val wikiPR = retry(10)(Factory.createResource("cz.vse.lhd.hypernymextractor.builder.CorpusBuilderPR2", featureMap).asInstanceOf[ProcessingResource])()
    val cPipeline = Factory.createResource("gate.creole.SerialController").asInstanceOf[SerialController]
    cPipeline.add(wikiPR)
    cPipeline
  }.execute()

  def newFeatureMap(offset: Int, limit: Int, dbpediaLinker: DBpediaLinker) = monitor.synchronized {
    val featureMap = Factory.newFeatureMap
    for (i <- 0 until list.getLength) {
      val paramName = list.item(i).getAttributes.getNamedItem("NAME").getTextContent
      val paramVal = list.item(i).getAttributes.getNamedItem("DEFAULT").getTextContent
      featureMap.put(paramName, paramVal)
    }
    featureMap.put("disambiguations", disambiguations)
    featureMap.put("dbpediaLinker", dbpediaLinker)
    featureMap.put("startPosInArticleNameList", "" + offset)
    featureMap.put("endPosInArticleNameList", "" + (offset + limit))
    featureMap
  }

}
