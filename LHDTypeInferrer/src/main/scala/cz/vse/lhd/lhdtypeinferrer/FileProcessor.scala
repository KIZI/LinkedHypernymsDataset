package cz.vse.lhd.lhdtypeinferrer

import com.hp.hpl.jena.rdf.model.ModelFactory
import com.hp.hpl.jena.rdf.model.Statement
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URLDecoder
import scala.io.Source
import scala.util.Try

object FileProcessor {

  def apply(files: List[File], output: List[String]) = if (files.isEmpty || output.isEmpty)
    None
  else
    Some(new FileProcessor(files, output map (x => new FileOutputStream(x)), None, _ => true))

}

class FileProcessor private (
  files: List[File],
  output: List[OutputStream],
  _stats: Option[Map[String, Int]],
  _filter: Statement => Boolean
) {

  def enableStats(st: Map[String, Int]) = new FileProcessor(files, output, Some(st), _filter)
  def filter(fi: Statement => Boolean) = new FileProcessor(files, output, _stats, fi)

  lazy val stats = _stats match {
    case Some(x) => x
    case None => Map.empty
  }

  def process: FileProcessor = files match {
    case head :: tail => new FileProcessor(
      tail,
      output,
      {
        val source = Source.fromFile(head)
        try {
          source.getLines.foldLeft(_stats)(writeStatement)
        } finally {
          source.close
        }
      },
      _filter
    ).process
    case _ => {
      output foreach (_.close)
      this
    }
  }

  private def writeStatement(stats: Option[Map[String, Int]], line: String) = {
    val model = ModelFactory.createDefaultModel
    try {
      model.read(new ByteArrayInputStream(line.toString.getBytes), null, "N-TRIPLE")
      if (model.size > 0) {
        val stmt = {
          val stmt = model.listStatements.next
          val x = stmt.getSubject.getURI
          val nstmt = model.createStatement(
            model.createResource(Try(URLDecoder.decode(x, "UTF-8")).getOrElse(x)),
            model.createProperty(stmt.getPredicate.getURI),
            model.createResource(stmt.getObject.asResource.getURI)
          )
          model.remove(stmt)
          model.add(nstmt)
          nstmt
        }
        if (_filter(stmt)) {
          output foreach (x => model.write(x, "N-TRIPLE"))
          val tripleObject = model.listStatements.next.getObject.asResource.getURI
          stats map (x => x + (tripleObject -> (x.getOrElse(tripleObject, 0) + 1)))
        } else {
          stats
        }
      } else {
        stats
      }
    } catch {
      case e: com.hp.hpl.jena.shared.JenaException => stats
    }
  }

}