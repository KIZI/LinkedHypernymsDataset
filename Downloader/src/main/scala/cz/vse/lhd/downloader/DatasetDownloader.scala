package cz.vse.lhd.downloader

trait DatasetDownloader {
  this: FileDownloader =>
  def download: Unit
}
