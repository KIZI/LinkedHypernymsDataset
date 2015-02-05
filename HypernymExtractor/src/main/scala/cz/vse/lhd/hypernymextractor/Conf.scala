package cz.vse.lhd.hypernymextractor

import cz.vse.lhd.core.AppConf
import cz.vse.lhd.core.ConfGlobal
import cz.vse.lhd.core.FileExtractor
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import cz.vse.lhd.core.Dir
import java.net.HttpURLConnection
import java.net.URL

object Conf extends ConfGlobal {

  val (
    globalPropertiesFile,
    gateDir,
    gatePluginLhdDir,
    gateJapeGrammar,
    memcachedAddress,
    memcachedPort,
    indexDir,
    wikiApi
    ) = {
    val prop = buildProp(new FileInputStream(AppConf.args(0).replaceFirst("""^["]""", "")))
    (
      prop.getProperty("global.properties.file"),
      prop.getProperty("gate.dir") /: Dir,
      prop.getProperty("gate.plugin.lhd.dir") /: Dir,
      prop.getProperty("gate.jape.grammar"),
      prop.getProperty("memcached.address"),
      prop.getProperty("memcached.port"),
      prop.getProperty("index.dir") /: Dir,
      prop.getProperty("wiki.api"))
  }

  val (
    datasetShort_abstractsPath,
    datasetLabelsPath,
    datasetInstance_typesPath,
    datasetDisambiguations) = (
    s"${Conf.datasetsDir}short_abstracts_$lang.nt",
    s"${Conf.datasetsDir}labels_$lang.nt",
    s"${Conf.datasetsDir}instance_types_$lang.nt",
    s"${Conf.datasetsDir}disambiguations_$lang.nt")

  List(gateJapeGrammar, datasetShort_abstractsPath, datasetLabelsPath, datasetInstance_typesPath) foreach {
    case FileExtractor(_) =>
    case x => throw new IOException(s"File $x does not exist or is not writable.")
  }

  {
    val of = new File(Conf.outputDir)
    if (!of.isDirectory) of.mkdir
    val conn = new URL(wikiApi).openConnection.asInstanceOf[HttpURLConnection]
    try {
      conn.connect
      if (conn.getResponseCode != HttpURLConnection.HTTP_OK)
        throw new IOException(s"WikiAPI $wikiApi is not reachable.")
    } finally {
      conn.disconnect
    }
  }

}

class ProcessStatus private (step: Int, end: Int) extends cz.vse.lhd.hypernymextractor.builder.ProcessStatus {
  def this(end: Int) = this(0, end)
  def ++ = new ProcessStatus(step + 1, end)
  def plusplus = ++
  def tryPrint = if (step % 1000 == 0)
    Logger.get.info(s"$step of $end resources extracted: " + ((step.toDouble / end) * 100).round + "%")
}

object Logger extends cz.vse.lhd.core.Logger {
  val conf = Conf
}