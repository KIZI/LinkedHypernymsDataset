package cz.vse.lhd.downloader

import java.net.URL

trait FileDownloader {

  def downloadFile(source: URL, dataset: Dataset, targetName: String): Unit

}
