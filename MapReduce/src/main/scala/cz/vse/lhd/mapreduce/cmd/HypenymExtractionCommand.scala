package cz.vse.lhd.mapreduce.cmd

import cz.vse.lhd.mapreduce.Conf
import cz.vse.lhd.mapreduce.Logger

class HypenymExtractionCommand(
  startPointer : Int,
  endPointer : Int
) extends Command {

  def execute = {
    Logger.get.info(s"New extraction process has been started: ${startPointer} - ${endPointer}")
    MavenCommandReceiver(s"""scala:run -Dlauncher=runner -DaddArgs="${Conf.globalPropertiesFile}|$startPointer|$endPointer" """.trim, HypenymExtractionCommand.homeDir)
    Logger.get.info(s"The extraction process has been finished: ${startPointer} - ${endPointer}")
  }
  
}

object HypenymExtractionCommand {
  
  val homeDir = "../HypernymExtractor"
  
}
