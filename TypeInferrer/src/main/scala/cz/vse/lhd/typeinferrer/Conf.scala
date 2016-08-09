package cz.vse.lhd.typeinferrer

import cz.vse.lhd.core.AppConf
import cz.vse.lhd.core.ConfGlobal
import cz.vse.lhd.core.FileExtractor
import java.io.IOException

object Conf extends ConfGlobal {

  val globalPropertiesFile = AppConf.args(0)

  val indexDir = config.get[String]("LHD.TypeInferrer.index-dir")

  val (
    datasetInstance_typesPath,
    datasetInstance_typesTransitivePath,
    datasetInstance_typesEnPath,
    datasetInstance_typesTransitiveEnPath,
    datasetOntologyPath) = (
    s"${Conf.datasetsDir}instance_types_$lang.nt",
    s"${Conf.datasetsDir}instance_types_transitive_$lang.nt",
    s"${Conf.datasetsDir}instance_types_en.nt",
    s"${Conf.datasetsDir}instance_types_transitive_en.nt",
    s"${Conf.datasetsDir}dbpedia.owl"
  )

  List(datasetInstance_typesPath, datasetInstance_typesTransitivePath, datasetOntologyPath) foreach {
    case FileExtractor(_) =>
    case x => throw new IOException(s"File $x does not exist or is not writable.")
  }

}