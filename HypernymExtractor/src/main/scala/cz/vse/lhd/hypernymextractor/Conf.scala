package cz.vse.lhd.hypernymextractor

import cz.vse.lhd.core.ConfGlobal
import java.io.FileInputStream

object Conf extends ConfGlobal {

  val globalPropertiesFile = "../global.properties"
  
  val (
    gateDir,
    gatePluginLhdDir,
    memcachedAddress,
    memcachedPort,
    indexDir,
    datasetShort_abstractsPath,
    datasetLabelsPath,
    datasetInstance_typesPath
  ) = {
    val prop = buildProp(new FileInputStream("modul.properties"))
    (
      prop.getProperty("gate.dir"),
      prop.getProperty("gate.plugin.lhd.dir"),
      prop.getProperty("memcached.address"),
      prop.getProperty("memcached.port"),
      prop.getProperty("index.dir"),
      prop.getProperty("dataset.short_abstracts.path"),
      prop.getProperty("dataset.labels.path"),
      prop.getProperty("dataset.instance_types.path")
    )
  }

}

object Logger extends cz.vse.lhd.core.Logger {
  val conf = Conf
}