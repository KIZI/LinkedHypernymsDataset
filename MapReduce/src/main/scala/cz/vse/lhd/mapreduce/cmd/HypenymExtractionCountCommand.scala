package cz.vse.lhd.mapreduce.cmd

import cz.vse.lhd.mapreduce.Conf

class HypenymExtractionCountCommand(callback : Int => Unit) extends Command {

  def execute = {
    val Pattern = """(?s).*?(\d+)\s*$""".r
    callback(
      MavenCommandReceiver !== (s"scala:run -Dlauncher=stats -DaddArgs=${Conf.globalPropertiesFile}", HypenymExtractionCommand.homeDir) match {
        case Pattern(c) => c.toInt
        case _ => 0
      }
    )
  }
  
}
