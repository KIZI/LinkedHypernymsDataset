package cz.vse.lhd.lhdnormalizer

import com.hp.hpl.jena.query.ARQ
import com.hp.hpl.jena.rdf.model.ModelFactory
import cz.vse.lhd.core.FileExtractor
import java.io.FileOutputStream
import java.io.PrintWriter
import scala.cz.vse.lhd.core.Match
import scala.io.Source

object RawToLod {

  def main(args: Array[String]): Unit = {
    import scala.collection.JavaConversions._
    ARQ.init
    Match(args) {
      case Array(FileExtractor(file), lang) if Array("en", "nl", "de").exists(_ == lang) => {
        val norLang = if (lang == "en") "" else s"$lang."
        val fileOutputStream = new FileOutputStream(file.getAbsolutePath + ".output")
        val fileOutputWriter = new PrintWriter(fileOutputStream, true)
        val source = Source.fromFile(file)
        try {
          val HypTuplePattern = "(.+);(.+)".r
          for (HypTuplePattern(subject, literal) <- source.getLines) {
            val model = ModelFactory.createDefaultModel
            model.add(
              model.createStatement(
                model.createResource(s"http://${norLang}dbpedia.org/resource/" + subject.replace(" ", "_")),
                model.createProperty(s"http://${norLang}dbpedia.org/property/hypernym"),
                model.createLiteral(literal, lang)))
            model.write(fileOutputStream, "N-TRIPLE")
          }
        } finally {
          fileOutputWriter.close
          source.close
        }
      }
    }
  }

}
