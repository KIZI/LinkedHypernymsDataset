package cz.vse.lhd.lhdontologycleanup

import cz.vse.lhd.core.AppConf
import cz.vse.lhd.core.ConfGlobal
import cz.vse.lhd.core.FileExtractor
import java.io.FileInputStream
import java.io.IOException

object Conf extends ConfGlobal {

  val (
    globalPropertiesFile,
    manualmappingOverridetypesPath,
    manualmappingExcludetypesPath
    ) = {
    val prop = buildProp(new FileInputStream(AppConf.args(0)))
    (
      prop.getProperty("global.properties.file"),
      prop.getProperty("manualmapping.overridetypes.path"),
      prop.getProperty("manualmapping.excludetypes.path"))
  }

  val (
    datasetInstance_typesPath,
    datasetInstance_typesEnPath,
    datasetOntologyPath,
    datasetInterlanguage_linksEnPath) = (
    s"${Conf.datasetsDir}instance_types_$lang.nt",
    s"${Conf.datasetsDir}instance_types_en.nt",
    s"${Conf.datasetsDir}dbpedia_${Conf.dbpediaVersion}.owl",
    s"${Conf.datasetsDir}interlanguage_links_en.nt")

  List(datasetInstance_typesPath, datasetInstance_typesEnPath, datasetOntologyPath, datasetInterlanguage_linksEnPath) foreach {
    case FileExtractor(_) =>
    case x => throw new IOException(s"File $x does not exist or is not writable.")
  }

}

object Logger extends cz.vse.lhd.core.Logger {
  val conf = Conf
}