package cz.vse.lhd.core

import scala.util.Try

object IsTrue {
  def unapply(str: String) = str match {
    case "true" | "1" => true
    case _ => false
  }
}

object AnyToInt {
  def unapply(a: Any) = Try(a.toString.toInt).toOption
}