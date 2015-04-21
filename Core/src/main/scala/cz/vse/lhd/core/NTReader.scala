package cz.vse.lhd.core

import java.io.{File, StringReader}
import java.net.URLDecoder

import com.hp.hpl.jena.rdf.model.{Model, ModelFactory, Statement}
import cz.vse.lhd.core.BasicFunction._
import org.apache.jena.riot.RiotException
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.io.Source
import scala.util.Try

/**
 * Created by propan on 9. 4. 2015.
 */
object NTReader extends App {

  lazy val logger = LoggerFactory.getLogger("cz.vse.lhd.core.NTReader")

  private def buildModelFromSeq(lines: Seq[String]): Model = {
    val model = ModelFactory.createDefaultModel
    try {
      model.read(new StringReader(lines.mkString("\n")), null, "N-TRIPLE")
    } catch {
      case e: RiotException if lines.size > 1 =>
        for (modelPart <- lines.map(line => buildModelFromSeq(Seq(line)))) {
          model.add(modelPart)
        }
      case e: RiotException if lines.size == 1 =>
        logger.warn("Invalid triple: " + lines.head)
    }
    model
  }

  def fromIterator(it: Iterator[String]) = it
    .grouped(500)
    .map(buildModelFromSeq)
    .flatMap { model =>
    model.listStatements.asScala map { stmt =>
      val subject = stmt.getSubject.getURI
      model.createStatement(
        model.createResource(Try(URLDecoder.decode(subject, "UTF-8")).getOrElse(subject)),
        model.createProperty(stmt.getPredicate.getURI),
        stmt.getObject
      )
    }
  }

  def fromSource(source: Source)(fn: Iterator[Statement] => Unit) = tryClose(source)(x => fn(fromIterator(x.getLines())))

  def fromFile(file: File) = fromSource(Source.fromFile(file, "UTF-8")) _

}
