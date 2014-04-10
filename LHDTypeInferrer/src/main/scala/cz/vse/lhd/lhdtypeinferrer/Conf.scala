package cz.vse.lhd.lhdtypeinferrer

import cz.vse.lhd.core.AppConf
import cz.vse.lhd.core.ConfGlobal
import java.io.FileInputStream

object Conf extends ConfGlobal {
  
  val (
    globalPropertiesFile,
    datasetInstance_typesPath,
    datasetOntologyPath,
    compressTemporaryFiles
  ) = {
    import scala.cz.vse.lhd.core.StringConversion._
    val prop = buildProp(new FileInputStream(AppConf.args(0)))
    (
      prop.getProperty("global.properties.file"),
      prop.getProperty("dataset.instance_types.path"),
      prop.getProperty("dataset.ontology.path"),
      prop.getProperty("compressTemporaryFiles").isTrue
    )
  }
  
}

object Logger extends cz.vse.lhd.core.Logger {
  val conf = Conf
}