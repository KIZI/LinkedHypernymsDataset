package cz.vse.lhd.mapreduce.cmd

import cz.vse.lhd.mapreduce.Conf
import cz.vse.lhd.mapreduce.Logger
import java.io.File

object MavenCommandReceiver {

  import scala.sys.process._
  
  val mvn = Conf.mavenCommand
  
  private def buildCommand(command : String, dir : String) = Process(s"${mvn} -q $command", new File(dir))
  
  def !==(command : String, dir : String) = {
    val result = buildCommand(command, dir).!!
    result
  }
  
  def apply(command : String, dir : String) : Unit = {
    val exitCode = buildCommand(command, dir).!
    Logger.get.info(s"$mvn $command: has been finished successfully (code: $exitCode)!")
    if (exitCode == 1) {
      Logger.get.info("Retry")
      apply(command, dir)
    }
  }
  
}
