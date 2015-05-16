package cz.vse.lhd.lhdontologycleanup.output

import java.io.{InputStream, OutputStream, PrintWriter}

import cz.vse.lhd.core.BasicFunction._

import scala.io.Source

/**
 * Created by propan on 15. 5. 2015.
 */
object UniqueLinesOutput {

  def apply(input: InputStream, output: OutputStream) = tryCloses(Source.fromInputStream(input, "UTF-8"), new PrintWriter(output)) {
    case Seq(source: Source, pw: PrintWriter) =>
      for (line <- source.getLines().toSet)
        pw.println(line)
  }

}
