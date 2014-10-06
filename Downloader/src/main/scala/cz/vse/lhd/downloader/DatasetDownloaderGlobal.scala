package cz.vse.lhd.downloader

import java.io.File
import java.net.URL

class DatasetDownloaderGlobal(version: String) extends DatasetDownloader {

  this: FileDownloader =>

  val urlBase = s"http://data.dws.informatik.uni-mannheim.de/dbpedia/$version/"
  val urlBaseLang = urlBase + Downloader.Conf.lang + "/"
  val urlBaseEn = urlBase + "en/"
  val files = (Set(
    "labels_" + Downloader.Conf.lang + ".nt.bz2",
    "short_abstracts_" + Downloader.Conf.lang + ".nt.bz2",
    "instance_types_" + Downloader.Conf.lang + ".nt.bz2") map (urlBaseLang + _)) ++ (Set(
      "instance_types_en.nt.bz2",
      "interlanguage_links_en.nt.bz2") map (urlBaseEn + _)) + (urlBase + s"dbpedia_$version.owl.bz2")
  val datasetsStrDir = Downloader.Conf.datasetsDir

  def download = for {
    file <- files
    strFile = file.replaceAll(".*/", "").stripSuffix(".bz2")
    fileFile = new File(datasetsStrDir + strFile)
  } {
    downloadFile(new URL(file), fileFile, BZ2)
  }

}
