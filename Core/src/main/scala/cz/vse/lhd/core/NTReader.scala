package cz.vse.lhd.core

import java.io.{File, ByteArrayInputStream}
import java.net.URLDecoder

import com.hp.hpl.jena.rdf.model.ModelFactory

import scala.io.Source
import scala.util.Try

/**
 * Created by propan on 9. 4. 2015.
 */
object NTReader {

  def fromIterator(it: Iterator[String]) = for (
    line <- it;
    model = ModelFactory.createDefaultModel().read(new ByteArrayInputStream(line.getBytes), null, "N-TRIPLE")
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

  def fromSource(source: Source) = {
    import BasicConversion.ClosableSource
    fromIterator(source.getLinesClosable)
  }

  def fromFile(file: File) = fromSource(Source.fromFile(file, "UTF-8"))

}
