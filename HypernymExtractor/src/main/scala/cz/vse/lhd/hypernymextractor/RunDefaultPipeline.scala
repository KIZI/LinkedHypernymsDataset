package cz.vse.lhd.hypernymextractor

import java.io.{BufferedReader, DataInputStream, File, FileInputStream, InputStreamReader}
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.{XPathConstants, XPathFactory}

import cz.vse.lhd.core.BasicFunction._
import cz.vse.lhd.core.{AppConf, NTReader}
import cz.vse.lhd.hypernymextractor.builder.DBpediaLinker
import gate.{Factory, Gate, ProcessingResource}
import gate.creole.SerialController
import org.slf4j.LoggerFactory
import org.w3c.dom.NodeList
import org.xml.sax.InputSource

import scala.io.Source

object RunDefaultPipeline extends AppConf {

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

  tryClose(new DBpediaLinker(Conf.wikiApi, Conf.lang, Conf.memcachedAddress, Conf.memcachedPort.toInt)) {
    dbpediaLinker =>
      logger.info("Cache has been connected successfully.")
      val disambiguations = tryClose(Source.fromFile(Conf.datasetDisambiguations, "UTF-8")) {
        source =>
          NTReader.fromIterator(source.getLines()).map(_.getSubject.getURI).toSet
      }
      logger.info(s"Disambiguations dataset has been loaded with size ${disambiguations.size}")

      val featureMap = Factory.newFeatureMap
      val list = XPathFactory.newInstance.newXPath.compile("//PARAMETER").evaluate(
        DocumentBuilderFactory.newInstance.newDocumentBuilder.parse(new InputSource(
          new BufferedReader(new InputStreamReader(
            new DataInputStream(new FileInputStream(Conf.gatePluginLhdDir + "/creole.xml")))))),
        XPathConstants.NODESET).asInstanceOf[NodeList]
      for (i <- 0 until list.getLength) {
        val paramName = list.item(i).getAttributes.getNamedItem("NAME").getTextContent
        val paramVal = list.item(i).getAttributes.getNamedItem("DEFAULT").getTextContent
        featureMap.put(paramName, paramVal)
      }
      featureMap.put("dbpediaLinker", dbpediaLinker)
      featureMap.put("disambiguations", disambiguations)
      AppConf.args match {
        case Array(_, start, end) =>
          featureMap.put("startPosInArticleNameList", start)
          featureMap.put("endPosInArticleNameList", end)
        case _ =>
      }

      val wikiPR = Factory.createResource("cz.vse.lhd.hypernymextractor.builder.CorpusBuilderPR2", featureMap).asInstanceOf[ProcessingResource]
      val cPipeline = Factory.createResource("gate.creole.SerialController").asInstanceOf[SerialController]
      cPipeline.add(wikiPR)
      cPipeline.execute()

  }

}
