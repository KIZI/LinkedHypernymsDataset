package cz.vse.lhd.ontologycleanup.output

import java.io.{FileOutputStream, File, OutputStream, PrintWriter}

import cz.vse.lhd.core.BasicFunction._
import cz.vse.lhd.core.Match

/**
 * Created by propan on 16. 5. 2015.
 */
trait OutputMaker {
  def makeFile(output: OutputStream): Unit
}

trait OutputMakerHeader {
  val header: String
}

trait OutputMakerFooter {
  val footer: String
}

object OutputMaker {

  private def addLineToOutput(output: OutputStream, line: String) = tryClose(new PrintWriter(output, true)) { pw =>
    pw.println(line)
  }

  private def makeAndProcessOutputStream(file: File)(process: OutputStream => Unit) = tryClose(new FileOutputStream(file, true))(process)

  def process(output: File, maker: OutputMaker) = {
    if (output.isFile)
      output.delete()
    makeAndProcessOutputStream(output) { outputStream =>
      Match(maker) {
        case maker: OutputMakerHeader => addLineToOutput(outputStream, maker.header)
      }
    }
    makeAndProcessOutputStream(output) { outputStream =>
      maker.makeFile(outputStream)
    }
    makeAndProcessOutputStream(output) { outputStream =>
      Match(maker) {
        case maker: OutputMakerFooter => addLineToOutput(outputStream, maker.footer)
      }
    }
  }

}