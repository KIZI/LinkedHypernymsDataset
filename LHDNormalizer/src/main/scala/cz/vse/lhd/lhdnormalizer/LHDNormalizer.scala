package cz.vse.lhd.lhdnormalizer

import com.hp.hpl.jena.query.ARQ
import com.hp.hpl.jena.rdf.model.ModelFactory
import cz.vse.lhd.core.FileExtractor
import cz.vse.lhd.core.Match
import java.io.ByteArrayInputStream
import java.io.FileOutputStream
import java.io.PrintWriter
import scala.io.Source

object LHDNormalizer {

  def main(args : Array[String]) : Unit = {
    import scala.collection.JavaConversions._
    ARQ.init
    args.headOption foreach (Match(_) {
        case FileExtractor(file) => {
            val fileOutputStream = new FileOutputStream(file.getAbsolutePath + ".output")
            val fileOutputWriter = new PrintWriter(fileOutputStream, true)
            val source = Source.fromFile(file)
            try {
              for (line <- source.getLines) {
                val model = ModelFactory.createDefaultModel
                try {
                  model.read(new ByteArrayInputStream(line.toString.getBytes), null, "N-TRIPLE")
                } catch {
                  case e : com.hp.hpl.jena.shared.JenaException =>
                }
                if (model.size > 0)
                  model.write(fileOutputStream, "N-TRIPLE")
                else
                  fileOutputWriter println line
              }
            } finally {
              fileOutputWriter.close
              source.close
            }
          }
      })
  }
  
}
