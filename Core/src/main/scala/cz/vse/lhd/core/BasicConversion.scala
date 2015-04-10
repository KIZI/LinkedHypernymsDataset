package cz.vse.lhd.core

/**
 * Created by propan on 9. 4. 2015.
 */
object BasicConversion {

  implicit class StringConversion(str: String) {

    def isTrue = IsTrue.unapply(str)

  }

}
