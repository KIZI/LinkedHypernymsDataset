package cz.vse.lhd

/**
 * Created by propan on 6. 6. 2015.
 */
trait Task {

  def run(): Unit

}

trait TaskId {

  self: Task =>

  val id: Char

}

trait TaskCompleted {

  self: Task =>

  def isCompleted: Boolean

  def completed(is: Boolean): Unit

}