package cz.vse.lhd.downloader

import java.io.File
import java.net.URL
import org.jsoup.Jsoup
import scala.cz.vse.lhd.core.AnyToInt

class DatasetDownloaderDe(version: String) extends DatasetDownloader {

  this: FileDownloader =>

  val urlBase = s"http://data.dws.informatik.uni-mannheim.de/dbpedia/$version/"
  val urlBaseDe = "http://de.dbpedia.org/downloads/"
  val filePatterns = List(
    "short-abstracts.nt.gz" -> "short_abstracts_de.nt",
    "labels.nt.gz" -> "labels_de.nt",
    "instance-types.nt.gz" -> "instance_types_de.nt")
  val urlBaseEn = urlBase + "en/"
  val enFiles = (Set("instance_types_en.nt.bz2", "interlanguage_links_en.nt.bz2") map (urlBaseEn + _)) + (urlBase + s"dbpedia_$version.owl.bz2")
  val datasetsStrDir = Downloader.Conf.outputDir + "../datasets/"

  def download = {
    import scala.collection.JavaConversions._
    val lastDump = Jsoup
      .connect(urlBaseDe)
      .get
      .select("a")
      .map(_.attr("href").stripSuffix("/"))
      .collect { case AnyToInt(x) if x > 0 => x }
      .max
    val urlBaseCurrentDe = urlBaseDe + lastDump + "/"
    for {
      (sFile, tFile) <- filePatterns
      sourceFile = s"dewiki-$lastDump-$sFile"
      targetFile = new File(datasetsStrDir + tFile)
    } {
      try {
        downloadFile(new URL(urlBaseCurrentDe + sourceFile), targetFile, GZ)
      } catch {
        case e: java.io.FileNotFoundException => downloadFile(new URL(urlBaseDe + sourceFile), targetFile, GZ)
      }
    }
    for {
      file <- enFiles
      strFile = file.replaceAll(".*/", "").stripSuffix(".bz2")
      fileFile = new File(datasetsStrDir + strFile)
    } {
      downloadFile(new URL(file), fileFile, BZ2)
    }
  }

}
