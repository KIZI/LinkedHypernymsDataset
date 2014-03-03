package cz.vse.lhd.core

import java.util.logging.FileHandler
import java.util.logging.Level

trait Logger {
  
  val conf : ConfGlobal
  
  lazy val get = {
    val logger = java.util.logging.Logger.getGlobal
    logger.setLevel(Level.INFO)
    if (conf.loggingEnabled)
      logger.addHandler(new FileHandler(conf.loggingDir + "/logging." + System.currentTimeMillis + ".log"))
    logger
  }
  
}