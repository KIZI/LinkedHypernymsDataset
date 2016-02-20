package cz.vse.lhd.downloader

import org.apache.jena.riot.Lang

import scala.annotation.tailrec
import scala.language.implicitConversions

/**
 * Created by propan on 19. 2. 2016.
 */
case class Dataset(name: String, format: DatasetFormat, compression: Option[Compression])

case class DatasetPattern(name: String, formats: Set[DatasetFormat], compressions: Set[Compression]) {

  private def nameSet = {
    val charIterator = name.toIterator
    @tailrec
    def namesBuild(groups: List[(Boolean, List[String])] = List(false -> List(""))): Set[String] = if (charIterator.isEmpty) {
      groups.head._2.toSet
    } else charIterator.next() match {
      case '(' => namesBuild((false -> List("")) :: groups)
      case ')' =>
        val mergedParent = groups.tail.head._1 -> groups.tail.head._2.flatMap(x => groups.head._2.map(x + _))
        namesBuild(mergedParent :: groups.tail.tail)
      case '|' =>
        val extendedGroup = true -> ("" :: groups.head._2)
        namesBuild(extendedGroup :: groups.tail)
      case x =>
        val appendedGroup = {
          groups.head._1 -> (if (groups.head._1) (groups.head._2.head + x) :: groups.head._2.tail else groups.head._2.map(_ + x))
        }
        namesBuild(appendedGroup :: groups.tail)
    }
    namesBuild()
  }

  def toDatasets = nameSet.flatMap { name =>
    formats.flatMap { format =>
      compressions.foldLeft(Set(Dataset(name, format, None)))((set, compression) => set + Dataset(name, format, Some(compression)))
    }
  }

}

sealed trait DatasetFormat {
  def fileExtension: String
}

object DatasetFormat {

  implicit def datasetFormatToJenaLang(datasetFormat: DatasetFormat): Lang = datasetFormat match {
    case NTRIPLE => Lang.NT
    case TURTLE => Lang.TTL
    case OWL => Lang.RDFXML
  }

}


object NTRIPLE extends DatasetFormat {
  val fileExtension: String = "nt"
}

object TURTLE extends DatasetFormat {
  val fileExtension: String = "ttl"
}

object OWL extends DatasetFormat {
  val fileExtension: String = "owl"
}

sealed trait Compression {
  def fileExtension: String
}

object BZ2 extends Compression {
  def fileExtension: String = "bz2"
}

object GZ extends Compression {
  def fileExtension: String = "gz"
}