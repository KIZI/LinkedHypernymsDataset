package cz.vse.lhd.lhdontologycleanup

import cz.vse.lhd.core.AppConf
import cz.vse.lhd.core.ConfGlobal
import java.io.FileInputStream

object Conf extends ConfGlobal {
  
  val (
    globalPropertiesFile,
    datasetInstance_typesPath,
    datasetInstance_typesEnPath,
    datasetOntologyPath,
    datasetInterlanguage_linksEnPath
  ) = {
    val prop = buildProp(new FileInputStream(AppConf.args(0)))
    (
      prop.getProperty("global.properties.file"),
      prop.getProperty("dataset.instance_types.path"),
      prop.getProperty("dataset.instance_types.en.path"),
      prop.getProperty("dataset.ontology.path"),
      prop.getProperty("dataset.interlanguage_links.en.path")
    )
  }
  
}

object Logger extends cz.vse.lhd.core.Logger {
  val conf = Conf
}