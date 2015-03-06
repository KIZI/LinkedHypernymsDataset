package cz.vse.lhd.core

object Test extends AppConf {

  val logger = new Logger {
    val conf = new ConfGlobal {
      val globalPropertiesFile = "../application.conf"
    }
  }
  println(AppConf.args.toList)
  println(logger.conf.outputDir)
  logger.get.info("test")
  
}