package cz.vse.lhd.downloader

import java.net.{HttpURLConnection, URL}

import cz.vse.lhd.downloader.DatasetDownloader.Exceptions.NoRemoteFileForDatasetPattern

import scala.util.{Failure, Try}

trait DatasetDownloader {

  this: FileDownloader =>

  def datasetPatterns: Map[String, DatasetPattern]

  def datasetToUrl(targetName: String, dataset: Dataset): URL

  def download(): Unit = {
    val downloadResults =
      for ((targetName, datasetPattern) <- datasetPatterns) yield {
        datasetPattern.toDatasets.find { dataset =>
          Try {
            val conn = datasetToUrl(targetName, dataset).openConnection().asInstanceOf[HttpURLConnection]
            conn.connect()
            val exists = conn.getResponseCode == 200
            conn.disconnect()
            if (!exists) throw new Exception
          }.isSuccess
        }.map { dataset =>
          Try(downloadFile(datasetToUrl(targetName, dataset), dataset, targetName))
        }.getOrElse(Failure(new NoRemoteFileForDatasetPattern(datasetPattern)))
      }
    downloadResults.collect {
      case Failure(ex) => ex
    }.reduceOption { (ex1, ex2) =>
      ex1.addSuppressed(ex2)
      ex1
    }.foreach(throw _)
  }

}

object DatasetDownloader {

  val compressions = Set(BZ2, GZ)
  val datasetFormats = Set(NTRIPLE, TURTLE)
  val ontologyFormats: Set[DatasetFormat] = Set(OWL)

  object Exceptions {

    class NoRemoteFileForDatasetPattern(datasetPattern: DatasetPattern) extends Exception(s"No remote file for this pattern: $datasetPattern")

  }

}
