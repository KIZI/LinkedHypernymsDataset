package cz.vse.lhd.downloader

import java.net.URL

class DatasetDownloaderGlobal(confData: Downloader.Conf.AutoConfData) extends DatasetAutoDownloader {

  this: FileDownloader =>

  import DatasetAutoDownloader._
  import Downloader.Conf._

  def urlBaseLang = confData.downloadBaseUrl + Downloader.Conf.lang + "/"

  def urlBaseEn = confData.downloadBaseUrl + "en/"

  val langDatasets = Map(
    s"disambiguations_$lang" -> s"disambiguations_$lang",
    s"short_abstracts_$lang" -> s"short(_|-)abstracts_$lang",
    s"instance_types_$lang" -> s"instance(_|-)types_$lang",
    s"instance_types_transitive_$lang" -> s"instance(_|-)types(_|-)transitive_$lang"
  )

  val enDatasets = Map(
    "instance_types_en" -> "instance(_|-)types_en",
    "instance_types_transitive_en" -> "instance(_|-)types(_|-)transitive_en",
    "interlanguage_links_en" -> "interlanguage(_|-)links_en"
  )

  val ontology = s"dbpedia_${confData.version}" -> s"(dbpedia_${confData.version}|ontology)"

  def datasetPatterns: Map[String, DatasetPattern] = ((langDatasets ++ enDatasets) map (x => x._1 -> DatasetPattern(x._2, datasetFormats, compressions))) + (ontology._1 -> DatasetPattern(ontology._2, ontologyFormats, compressions))

  def datasetToUrl(targetName: String, dataset: Dataset): URL = {
    val prefix = if (langDatasets.contains(targetName)) {
      urlBaseLang
    } else if (enDatasets.contains(targetName)) {
      urlBaseEn
    } else {
      confData.ontologyBaseUrl
    }
    new URL(prefix + dataset.name + "." + dataset.format.fileExtension + dataset.compression.map("." + _.fileExtension).getOrElse(""))
  }

}
