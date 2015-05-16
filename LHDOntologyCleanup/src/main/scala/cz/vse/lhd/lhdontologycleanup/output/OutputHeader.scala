package cz.vse.lhd.lhdontologycleanup.output

import java.io.{PrintWriter, OutputStream, InputStream}

import cz.vse.lhd.core.BasicFunction

/**
 * Created by propan on 15. 5. 2015.
 */
object OutputHeader {

  def apply[A](output: OutputStream, header: String)(outputProcess: OutputStream => A) = BasicFunction.tryClose(new PrintWriter(output, true)) { pw =>
    pw.println(header)
    outputProcess(output)
  }

}
