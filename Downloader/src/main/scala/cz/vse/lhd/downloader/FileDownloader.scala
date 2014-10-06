package cz.vse.lhd.downloader

import java.io.File
import java.net.URL

trait FileDownloader {
  def downloadFile(url: URL, target: File, fType: FileType): Unit
}
