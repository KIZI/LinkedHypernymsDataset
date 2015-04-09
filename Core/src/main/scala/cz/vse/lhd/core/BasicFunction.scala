package cz.vse.lhd.core

/**
 * Created by propan on 9. 4. 2015.
 */
object BasicFunction {

  import scala.language.reflectiveCalls

  def tryClose[A, B <: { def close(): Unit }](closeable: B)(f: B => A): A = try { f(closeable) } finally { closeable.close() }

  def tryCloseBool[A, B <: { def close(): Boolean }](closeable: B)(f: B => A): A = try { f(closeable) } finally { closeable.close() }

}
