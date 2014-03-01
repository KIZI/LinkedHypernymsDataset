package cz.vse.lhd.thdyagoduplicateremoval

import cz.vse.lhd.core.ConfGlobal
import java.io.FileInputStream

object Conf extends ConfGlobal {

  val globalPropertiesFile = "../global.properties"
  
  val (
    datasetYagoTaxonomyPath,
    datasetYagoTypesPath
  ) = {
    val prop = buildProp(new FileInputStream("modul.properties"))
    (
      prop.getProperty("dataset.yagoTaxonomy.path"),
      prop.getProperty("dataset.yagoTypes.path")
    )
  }
  
}

object Logger extends cz.vse.lhd.core.Logger {
  val conf = Conf
}