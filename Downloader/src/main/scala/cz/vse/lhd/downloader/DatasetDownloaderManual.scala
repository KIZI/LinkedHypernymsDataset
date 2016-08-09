package cz.vse.lhd.downloader

import java.net.URL

/**
  * Created by propan on 9. 8. 2016.
  */
class DatasetDownloaderManual(confData: Downloader.Conf.ManualConfData) extends DatasetDownloader {

  this: FileDownloader =>

  private def urlToDataset(url: String) = {
    val FileExtensions = ".+?(?:\\.([^.]+))?\\.([^.]+)$".r
    url match {
      case FileExtensions(exp1, exp2) =>
        val format = Set(NTRIPLE, TURTLE, OWL).find(x => x.fileExtension == exp1 || x.fileExtension == exp2)
        val compression = Set(BZ2, GZ).find(_.fileExtension == exp2)
        format.map(format => Dataset(url, format, compression))
      case _ => None
    }
  }

  private def datasets: Map[String, Dataset] = Map(
    "short_abstracts_" + Downloader.Conf.lang -> confData.shortAbstracts,
    "disambiguations_" + Downloader.Conf.lang -> confData.disambiguations,
    "instance_types" + Downloader.Conf.lang -> confData.instanceTypes,
    "instance_types_transitive" + Downloader.Conf.lang -> confData.instanceTypesTransitive,
    "instance_types_en" -> confData.instanceTypesEn,
    "instance_types_transitive_en" -> confData.instanceTypesTransitiveEn,
    "interlanguage_links_en" -> confData.interlanguageLinksEn,
    "dbpedia" -> confData.ontology
  ).mapValues(urlToDataset).collect {
    case (k, Some(v)) => k -> v
  }

  def download(): Unit = for ((targetName, dataset) <- datasets) {
    downloadFile(new URL(dataset.name), dataset, targetName)
  }

}
