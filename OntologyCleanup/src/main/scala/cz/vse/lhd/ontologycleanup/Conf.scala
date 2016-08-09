package cz.vse.lhd.ontologycleanup

import java.io.{File, IOException}

import cz.vse.lhd.core.{AppConf, ConfGlobal, FileExtractor}

object Conf extends ConfGlobal {

  val globalPropertiesFile = AppConf.args(0)

  val (
    indexDir,
    manualmappingOverridetypesPath,
    manualmappingExcludetypesPath
    ) = {
    (
      config.get[String]("LHD.OntologyCleanup.index-dir"),
      config.getOrElse[String]("LHD.OntologyCleanup.manualmapping.overridetypes-path", null),
      config.getOrElse[String]("LHD.OntologyCleanup.manualmapping.excludetypes-path", null)
      )
  }

  val (
    datasetInstance_typesPath,
    datasetInstance_typesTransitivePath,
    datasetInstance_typesEnPath,
    datasetInstance_typesTransitiveEnPath,
    datasetOntologyPath,
    datasetInterlanguage_linksEnPath) = (
    s"${Conf.datasetsDir}instance_types_$lang.nt",
    s"${Conf.datasetsDir}instance_types_transitive_$lang.nt",
    s"${Conf.datasetsDir}instance_types_en.nt",
    s"${Conf.datasetsDir}instance_types_transitive_en.nt",
    s"${Conf.datasetsDir}dbpedia.owl",
    s"${Conf.datasetsDir}interlanguage_links_en.nt"
    )

  List(
    datasetInstance_typesPath,
    datasetInstance_typesEnPath,
    datasetInstance_typesTransitivePath,
    datasetInstance_typesTransitiveEnPath,
    datasetOntologyPath,
    datasetInterlanguage_linksEnPath
  ) foreach {
    case FileExtractor(_) =>
    case x => throw new IOException(s"File $x does not exist or is not writable.")
  }

  for (of <- List(Conf.indexDir).map(new File(_)) if !of.isDirectory) {
    of.mkdirs
  }

}