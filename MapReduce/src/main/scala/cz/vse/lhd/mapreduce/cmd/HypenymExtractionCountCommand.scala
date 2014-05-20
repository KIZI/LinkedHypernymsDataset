package cz.vse.lhd.mapreduce.cmd

class HypenymExtractionCountCommand(callback : Int => Unit) extends Command {

  def execute = {
    val Pattern = """(?s).*?(\d+)\s*$""".r
    callback(
      MavenCommandReceiver !== ("scala:run -Dlauncher=stats -DaddArgs=module.properties", HypenymExtractionCommand.homeDir) match {
        case Pattern(c) => c.toInt
        case _ => 0
      }
    )
  }
  
}
