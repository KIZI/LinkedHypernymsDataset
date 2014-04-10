package scala.cz.vse.lhd.core

object Match {
  def default: PartialFunction[Any, Unit] = { case _ => }
  def apply[T](x: T)(body: PartialFunction[T, Unit]) = (body orElse default)(x)
}