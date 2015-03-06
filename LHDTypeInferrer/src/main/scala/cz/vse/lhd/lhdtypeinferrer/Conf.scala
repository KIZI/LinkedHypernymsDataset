package cz.vse.lhd.lhdtypeinferrer

import cz.vse.lhd.core.AppConf
import cz.vse.lhd.core.ConfGlobal
import cz.vse.lhd.core.FileExtractor
import java.io.IOException

object Conf extends ConfGlobal {

  val globalPropertiesFile = AppConf.args(0)

  val (
    compressTemporaryFiles
    ) = {
    (
      config.get[Boolean]("LHD.TypeInferrer.compressTemporaryFiles")
    )
  }

  val (
    datasetInstance_typesPath,
    datasetOntologyPath) = (
    s"${Conf.datasetsDir}instance_types_$lang.nt",
    s"${Conf.datasetsDir}dbpedia_${Conf.dbpediaVersion}.owl"
  )

  List(datasetInstance_typesPath, datasetOntologyPath) foreach {
    case FileExtractor(_) =>
    case x => throw new IOException(s"File $x does not exist or is not writable.")
  }

}

object Logger extends cz.vse.lhd.core.Logger {
  val conf = Conf
}