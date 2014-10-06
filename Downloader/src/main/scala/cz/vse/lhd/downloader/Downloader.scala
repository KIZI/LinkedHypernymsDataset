package cz.vse.lhd.downloader

import cz.vse.lhd.core.AppConf
import cz.vse.lhd.core.ConfGlobal
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream

object Downloader extends AppConf {

  object Conf extends ConfGlobal {
    val globalPropertiesFile = AppConf.args(0)
  }

  object Logger extends cz.vse.lhd.core.Logger {
    val conf = Conf
  }

  val datasetDownloader = {
    trait FileDownloaderImpl extends FileDownloader {
      def downloadFile(url: URL, target: File, fType: FileType) = {
        target.getParentFile match {
          case x: File if x.isDirectory && x.canWrite =>
          case x: File if !x.isDirectory => if (!x.mkdirs || !x.canWrite) throw new DownloaderException(s"Directory ${x.getAbsolutePath} couldn't be created!")
          case _ => throw new DownloaderException(s"Bad parent folder of ${target.getAbsolutePath}!")
        }
        if (target.isFile && !target.delete) throw new DownloaderException(s"File ${target.getAbsolutePath} cannot be deleted!")
        var i = 0L
        var time = System.currentTimeMillis
        val bis = new BufferedInputStream(fType match {
          case BZ2 => new BZip2CompressorInputStream(url.openStream(), true)
          case GZ => new GzipCompressorInputStream(url.openStream(), true)
        })
        val bos = new BufferedOutputStream(new FileOutputStream(target))
        Logger.get.info(s"The file: ${target.getName} is downloading...")
        try {
          for (bytes <- (Stream continually (bis.read) takeWhile (_ != -1))) {
            bos.write(bytes)
            i = i + 1
            if (i % 10000000 == 0) {
              val speed = ((10 / ((System.currentTimeMillis - time) / 1000.0)) * 1000).round
              Logger.get.info(s"The file: ${target.getName} is downloading... ${i / 1000000}MB downloaded (speed: ${speed}kB/s).")
              time = System.currentTimeMillis
            }
          }
        } finally {
          bos.close
          bis.close
        }
        Logger.get.info(s"The file: ${target.getName} is completely downloaded.")
      }
    }
    if (Conf.lang == "de")
      new DatasetDownloaderDe(Conf.dbpediaVersion) with FileDownloaderImpl
    else
      new DatasetDownloaderGlobal(Conf.dbpediaVersion) with FileDownloaderImpl
  }

  datasetDownloader.download

}

sealed trait FileType
object BZ2 extends FileType
object GZ extends FileType

class DownloaderException(m: String) extends Exception(m)