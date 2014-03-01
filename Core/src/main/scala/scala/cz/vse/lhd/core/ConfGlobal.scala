package cz.vse.lhd.core

import java.io.FileInputStream
import java.io.InputStream
import java.util.Properties

trait ConfGlobal {
  
  val globalPropertiesFile : String
  
  lazy val (
    outputDir,
    loggingDir,
    loggingEnabled,
    lang
  ) = {
    val prop = buildProp(new FileInputStream(globalPropertiesFile))
    (
      prop.getProperty("output.dir"),
      prop.getProperty("logging.dir"),
      prop.getProperty("logging.enabled") match {
        case "true" | "1" => true
        case _ => false
      },
      prop.getProperty("lang")
    )
  }
  
  protected def buildProp(is : InputStream) = {
    val prop = new Properties
    try {
      prop.load(is);
    } finally {
      is.close();
    }
    prop
  }
  
}