package cz.vse.lhd.core

object Test extends AppConf {

  val logger = new Logger {
    val conf = new ConfGlobal {
      val globalPropertiesFile = "../global.properties"
    }
  }
  logger.get.info("ahoj")
  
}