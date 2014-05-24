package scala.cz.vse.lhd.core

object IsTrue {

  def unapply(str: String) = str match {
    case "true" | "1" => true
    case _ => false
  }

}