package cz.vse.lhd.tasks

import java.io.File

import cz.vse.lhd.{Conf, Task, TaskCompleted}

/**
 * Created by propan on 6. 6. 2015.
 */
trait FileTaskCompleted extends TaskCompleted {

  self: Task =>

  val taskGroup: String

  lazy val completedFile = new File(Conf.outputDir + taskGroup + ".completed")

  def isCompleted: Boolean = completedFile.isFile

  def completed(create: Boolean): Unit = if (create && !isCompleted) {
    completedFile.createNewFile()
  } else if (!create && isCompleted) {
    completedFile.delete()
  }

}
