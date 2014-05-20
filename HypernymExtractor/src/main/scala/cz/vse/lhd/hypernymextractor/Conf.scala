package cz.vse.lhd.hypernymextractor

import cz.vse.lhd.core.AppConf
import cz.vse.lhd.core.ConfGlobal
import java.io.FileInputStream
import scala.cz.vse.lhd.core.Dir

object Conf extends ConfGlobal {
  
  val (
    globalPropertiesFile,
    gateDir,
    gatePluginLhdDir,
    gateJapeGrammar,
    memcachedAddress,
    memcachedPort,
    indexDir,
    datasetShort_abstractsPath,
    datasetLabelsPath,
    datasetInstance_typesPath,
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
      prop.getProperty("dataset.short_abstracts.path"),
      prop.getProperty("dataset.labels.path"),
      prop.getProperty("dataset.instance_types.path"),
      prop.getProperty("wiki.api")
    )
  }

}

class Loader private (step : Int, end : Int) {
  def this(end : Int) = this(0, end)
  def ++ = new Loader(step + 1, end)
  def tryPrint = if (step % 1000 == 0)
    Logger.get.info(s"$step of $end resources extracted: " + ((step.toDouble / end) * 100).round + "%")
}

object Logger extends cz.vse.lhd.core.Logger {
  val conf = Conf
}