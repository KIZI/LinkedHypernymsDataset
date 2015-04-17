package cz.vse.lhd.hypernymextractor

import cz.vse.lhd.core.AppConf
import gate.Factory
import gate.Gate
import gate.ProcessingResource
import gate.creole.SerialController
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import org.w3c.dom.NodeList
import org.xml.sax.InputSource

object RunDefaultPipeline extends AppConf {

  //Gate.init(); 
  val gateHomeFile = new File(Conf.gateDir)
  Gate.setGateHome(gateHomeFile)
  val pluginsHomeFile = new File(Conf.gateDir.replaceAll("/+$", "") + "/plugins")
  Gate.setPluginsHome(pluginsHomeFile)
  Gate.init()
  Gate.getCreoleRegister.registerDirectories(new File(pluginsHomeFile, "ANNIE").toURI.toURL)
  Gate.getCreoleRegister.registerDirectories(new File(Conf.gatePluginLhdDir).toURI.toURL)
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
