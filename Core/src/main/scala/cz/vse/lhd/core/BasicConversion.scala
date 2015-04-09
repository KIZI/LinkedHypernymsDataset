package cz.vse.lhd.core

import scala.io.Source

/**
 * Created by propan on 9. 4. 2015.
 */
object BasicConversion {

  implicit class StringConversion(str: String) {

    def isTrue = IsTrue.unapply(str)

  }

  implicit class ClosableSource(source: Source) {

    def getLinesClosable: Iterator[String] = new Iterator[String] {
      val it = source.getLines()
      closeIfEmpty()

      private def closeIfEmpty() = if (!it.hasNext) source.close()

      def hasNext = it.hasNext

      def next() = {
        val result = it.next()
        closeIfEmpty()
        result
      }
    }

  }

}
