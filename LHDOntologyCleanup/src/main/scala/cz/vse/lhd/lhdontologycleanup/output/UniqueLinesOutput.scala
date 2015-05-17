package cz.vse.lhd.lhdontologycleanup.output

import java.io.{OutputStream, File, PrintWriter}

import cz.vse.lhd.core.BasicFunction._

import scala.io.Source

/**
 * Created by propan on 15. 5. 2015.
 */
class UniqueLinesOutput(input: File) extends OutputMaker with OutputMakerHeader {

  val header = "# Input file with duplicate lines removed"

  def makeFile(output: OutputStream) = tryClose(Source.fromFile(input, "UTF-8")) { source =>
    tryClose(new PrintWriter(output)) { pw =>
      for (line <- source.getLines().toSet[String])
        pw.println(line)
    }
  }

}
