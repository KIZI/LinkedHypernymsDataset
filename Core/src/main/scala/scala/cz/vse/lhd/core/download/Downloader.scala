package scala.cz.vse.lhd.core.download

import cz.vse.lhd.core.AppConf
import cz.vse.lhd.core.ConfGlobal
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream

object Downloader extends AppConf {

  object Conf extends ConfGlobal {
    val globalPropertiesFile = AppConf.args(0)
  }
  
  val urlBase = "http://downloads.dbpedia.org/3.9/"
  val urlBaseLang = urlBase + Conf.lang + "/"
  val urlBaseEn = urlBase + "en/"
  val files = (
    Set(
      "labels_" + Conf.lang + ".nt.bz2",
      "short_abstracts_" + Conf.lang + ".nt.bz2",
      "instance_types_" + Conf.lang + ".nt.bz2"
    ) map (urlBaseLang + _)
  ) ++ (
    Set(
      "instance_types_en.nt.bz2",
      "interlanguage_links_en.nt.bz2"
    ) map (urlBaseEn + _)
  ) + (urlBase + "dbpedia_3.9.owl.bz2")
  
  val datasetsStrDir = Conf.outputDir + "../datasets/"
  val datasetsDir = new File(datasetsStrDir)
  if (!datasetsDir.isDirectory)
    datasetsDir.mkdir
  
  for {
    file <- files
    strFile = file.replaceAll(".*/", "").stripSuffix(".bz2")
    fileFile = new File(datasetsStrDir + strFile)
    if !fileFile.isFile
  } {
    println(s"The file: $strFile is downloading...")
    var i = 0L
    var time = System.currentTimeMillis
    val bos = new BufferedOutputStream(new FileOutputStream(datasetsStrDir + strFile))
    val bis = new BufferedInputStream(new BZip2CompressorInputStream(new URL(file).openStream(), true))
    try {
      for (bytes <- (Stream continually (bis.read) takeWhile (_ != -1))) {
        bos.write(bytes)
        i = i + 1
        if (i % 10000000 == 0) {
          val speed = ((10 / ((System.currentTimeMillis - time) / 1000.0)) * 1000).round
          println(s"The file: $strFile is downloading... ${i/1000000}MB downloaded (speed: ${speed}kB/s).")
          time = System.currentTimeMillis
        }
      }
      println(s"The file: $strFile is completely downloaded.")
    } finally {
      bos.close
      bis.close
    }
  }
  
}
