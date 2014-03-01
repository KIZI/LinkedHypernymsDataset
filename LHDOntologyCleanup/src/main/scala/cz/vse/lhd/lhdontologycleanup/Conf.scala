package cz.vse.lhd.lhdontologycleanup

import cz.vse.lhd.core.ConfGlobal
import java.io.FileInputStream

object Conf extends ConfGlobal {

  val globalPropertiesFile = "../global.properties"
  
  val (
    datasetInstance_typesPath,
    datasetInstance_typesEnPath,
    datasetOntologyPath,
    datasetInterlanguage_linksPath
  ) = {
    val prop = buildProp(new FileInputStream("modul.properties"))
    (
      prop.getProperty("dataset.instance_types.path"),
      prop.getProperty("dataset.instance_types.en.path"),
      prop.getProperty("dataset.ontology.path"),
      prop.getProperty("dataset.interlanguage_links.path")
    )
  }
  
}

object Logger extends cz.vse.lhd.core.Logger {
  val conf = Conf
}