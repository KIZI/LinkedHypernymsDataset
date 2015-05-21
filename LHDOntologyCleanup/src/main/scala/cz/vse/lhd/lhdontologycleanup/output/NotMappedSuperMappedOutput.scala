package cz.vse.lhd.lhdontologycleanup.output

import java.io.{File, OutputStream}

import cz.vse.lhd.core.{BasicFunction, NTReader, NTWriter, RdfTriple}
import cz.vse.lhd.lhdontologycleanup.Conf

import scala.annotation.tailrec
import scala.io.Source

/**
 * Created by propan on 18. 5. 2015.
 */
class NotMappedSuperMappedOutput(mapped: File, notMapped: File) extends OutputMaker with OutputMakerHeader {

  val header: String = "# Hypernyms which don not have any DBpedia ontology but their super-hypernym has"

  @tailrec
  private def mapToSuperOntology(hypernym: String, mappedMap: Map[String, String], notMappedMap: Map[String, String])(implicit used: Set[String] = Set.empty): Option[String] = {
    val newUsed = used + hypernym
    mappedMap.get(hypernym) match {
      case Some(result) => Some(Conf.dbpediaOntologyUri + result)
      case None => notMappedMap.get(hypernym) match {
        case Some(superHypernym) if !newUsed(superHypernym) => mapToSuperOntology(superHypernym, mappedMap, notMappedMap)(newUsed)
        case _ => None
      }
    }
  }

  private def uriToName(resource: String) = resource.replaceAll(s"${Conf.dbpediaOntologyUri}|${Conf.dbpediaResourceUriRegexp}", "")

  def makeFile(output: OutputStream) = {
    val mappedMap = BasicFunction.tryClose(Source.fromFile(mapped, "UTF-8")) { source =>
      NTReader.fromIterator(source.getLines()).map(stmt => uriToName(stmt.getSubject.getURI) -> uriToName(stmt.getObject.asResource().getURI)).toMap
    }
    val notMappedMap = BasicFunction.tryClose(Source.fromFile(notMapped, "UTF-8")) { source =>
      NTReader.fromIterator(source.getLines()).map(stmt => uriToName(stmt.getSubject.getURI) -> uriToName(stmt.getObject.asResource().getURI)).toMap
    }
    NTReader.fromFile(notMapped) { it =>
      NTWriter.fromIterator(
        for {
          stmt <- it
          dbo <- mapToSuperOntology(uriToName(stmt.getObject.asResource().getURI), mappedMap, notMappedMap)
        } yield {
          RdfTriple(stmt.getSubject.getURI, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", dbo)
        },
        output
      )
    }
  }

}