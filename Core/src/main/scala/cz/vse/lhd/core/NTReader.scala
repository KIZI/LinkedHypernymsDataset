package cz.vse.lhd.core

import java.io.{ByteArrayInputStream, File}
import java.net.URLDecoder

import com.hp.hpl.jena.rdf.model.{ModelFactory, Statement}
import cz.vse.lhd.core.BasicFunction._
import org.apache.jena.riot.RiotException
import org.slf4j.LoggerFactory

import scala.io.Source
import scala.util.Try

/**
 * Created by propan on 9. 4. 2015.
 */
object NTReader {

  lazy val logger = LoggerFactory.getLogger("cz.vse.lhd.core.NTReader")

  def fromIterator[A](it: Iterator[String]) = for (
    line <- it;
    model = {
      val model = ModelFactory.createDefaultModel
      try {
        model.read(new ByteArrayInputStream(line.getBytes), null, "N-TRIPLE")
      } catch {
        case e: RiotException =>
          logger.warn("Invalid triple: " + line)
          model
      }
    }
    if !model.isEmpty
  ) yield {
      val stmt = model.listStatements.next
      val x = stmt.getSubject.getURI
      model.createStatement(
        model.createResource(Try(URLDecoder.decode(x, "UTF-8")).getOrElse(x)),
        model.createProperty(stmt.getPredicate.getURI),
        stmt.getObject
      )
    }

  def fromSource(source: Source)(fn: Iterator[Statement] => Unit) = tryClose(source)(x => fn(fromIterator(x.getLines())))

  def fromFile(file: File) = fromSource(Source.fromFile(file, "UTF-8")) _

}
