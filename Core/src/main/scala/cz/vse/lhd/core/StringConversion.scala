package cz.vse.lhd.core

object StringConversion {

  import scala.language.implicitConversions
  
  implicit def strToStringConversion(str : String) = new StringConversion(str)
  
}

class StringConversion(str : String) {
  
  def isTrue = IsTrue.unapply(str)
  
}
