package cz.vse.lhd.downloader

import java.net.URL

import cz.vse.lhd.core.AnyToInt
import cz.vse.lhd.downloader.DatasetDownloader._
import org.jsoup.Jsoup

import scala.collection.JavaConversions._

class DatasetDownloaderDe(version: String) extends DatasetDownloader {

  this: FileDownloader =>

  def urlBaseEn = Downloader.Conf.downloadBaseUrl + "en/"

  val urlBaseDe = "http://de.dbpedia.org/downloads/"

  val langDatasets = Map(
    "short_abstracts_de" -> "short-abstracts",
    "disambiguations_de" -> "disambiguations-unredirected",
    "instance_types_de" -> "instance-types",
    "instance_types_transitive_de" -> "instance-types-transitive"
  )

  val enDatasets = Map(
    "instance_types_en" -> "instance(_|-)types_en",
    "instance_types_transitive_en" -> "instance(_|-)types(_|-)transitive_en",
    "interlanguage_links_en" -> "interlanguage(_|-)links_en"
  )

  val ontology = s"dbpedia_$version" -> s"(dbpedia_$version|ontology)"

  lazy val lastDump = Jsoup
    .connect(urlBaseDe)
    .get
    .select("a")
    .map(_.attr("href").stripSuffix("/"))
    .collect { case AnyToInt(x) if x > 0 => x }
    .max

  def datasetPatterns: Map[String, DatasetPattern] = ((langDatasets ++ enDatasets) map (x => x._1 -> DatasetPattern(x._2, datasetFormats, compressions))) + (ontology._1 -> DatasetPattern(ontology._2, ontologyFormats, compressions))

  def datasetToUrl(targetName: String, dataset: Dataset): URL = {
    val nameWithPrefix = if (langDatasets.containsKey(targetName)) {
      urlBaseDe + lastDump + "/" + "dewiki-" + lastDump + "-" + langDatasets(dataset.name)
    } else if (enDatasets.containsKey(targetName)) {
      urlBaseEn + dataset.name
    } else {
      Downloader.Conf.ontologyBaseUrl + dataset.name
    }
    new URL(nameWithPrefix + "." + dataset.format.fileExtension + dataset.compression.map("." + _.fileExtension).getOrElse(""))
  }


}
