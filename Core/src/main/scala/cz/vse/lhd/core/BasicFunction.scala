package cz.vse.lhd.core

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

/**
 * Created by propan on 9. 4. 2015.
 */
object BasicFunction {

  import scala.language.reflectiveCalls

  def tryClose[A, B <: {def close() : Unit}](closeable: B)(f: B => A): A = try {
    f(closeable)
  } finally {
    closeable.close()
  }

  def tryCloses[A, B <: {def close() : Unit}](closeable: B*)(f: PartialFunction[Seq[B], A]): Option[A] = try {
    f.lift(closeable)
  } finally {
    closeable.foreach(_.close())
  }

  def tryCloseBool[A, B <: {def close() : Boolean}](closeable: B)(f: B => A): A = try {
    f(closeable)
  } finally {
    closeable.close()
  }

  @tailrec
  def retry[T](n: Int)(fn: => T)(implicit ffn: (Throwable, Int) => Unit = (_, _) => Unit): T = {
    Try {
      fn
    } match {
      case Success(x) => x
      case Failure(e) if n > 1 =>
        ffn(e, n)
        retry(n - 1)(fn)(ffn)
      case Failure(e) => throw e
    }
  }

}

object Match {
  def default: PartialFunction[Any, Unit] = {
    case _ =>
  }

  def apply[T](x: T)(body: PartialFunction[T, Unit]) = (body orElse default)(x)
}

object Lift {
  def default[U]: PartialFunction[Any, Option[U]] = {
    case _ => None
  }

  def apply[T, U](x: T)(body: PartialFunction[T, Option[U]]) = (body orElse default)(x)
}

object AutoLift {
  def apply[T, U](x: T)(body: PartialFunction[T, U]) = body.lift(x)
}