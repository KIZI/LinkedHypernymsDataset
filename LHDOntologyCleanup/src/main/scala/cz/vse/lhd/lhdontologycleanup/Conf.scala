package cz.vse.lhd.lhdontologycleanup

import cz.vse.lhd.core.AppConf
import cz.vse.lhd.core.ConfGlobal
import cz.vse.lhd.core.FileExtractor
import java.io.IOException

object Conf extends ConfGlobal {

  val globalPropertiesFile = AppConf.args(0)

  val (
    manualmappingOverridetypesPath,
    manualmappingExcludetypesPath
    ) = {
    (
      config.getOrElse[String]("LHD.OntologyCleanup.manualmapping.overridetypes.path", null),
      config.getOrElse[String]("LHD.OntologyCleanup.manualmapping.excludetypes.path", null)
    )
  }

  val (
    datasetInstance_typesPath,
    datasetInstance_typesEnPath,
    datasetOntologyPath,
    datasetInterlanguage_linksEnPath) = (
    s"${Conf.datasetsDir}instance_types_$lang.nt",
    s"${Conf.datasetsDir}instance_types_en.nt",
    s"${Conf.datasetsDir}dbpedia_${Conf.dbpediaVersion}.owl",
    s"${Conf.datasetsDir}interlanguage_links_en.nt"
  )

  List(datasetInstance_typesPath, datasetInstance_typesEnPath, datasetOntologyPath, datasetInterlanguage_linksEnPath) foreach {
    case FileExtractor(_) =>
    case x => throw new IOException(s"File $x does not exist or is not writable.")
  }

}

object Logger extends cz.vse.lhd.core.Logger {
  val conf = Conf
}