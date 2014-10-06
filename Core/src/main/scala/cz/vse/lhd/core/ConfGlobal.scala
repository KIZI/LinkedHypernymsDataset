package cz.vse.lhd.core

import java.io.FileInputStream
import java.io.InputStream
import java.util.Properties

trait AppConf extends DelayedInit {

  private var _body: () => Unit = _

  def delayedInit(body: => Unit) = {
    _body = body _
  }

  def main(args: Array[String]): Unit = {
    AppConf._args = args
    _body()
  }

}

object AppConf {

  private var _args: Array[String] = _

  lazy val args = _args

}

trait ConfGlobal {

  val globalPropertiesFile: String

  lazy val (
    outputDir,
    datasetsDir,
    lang,
    dbpediaVersion
    ) = {
    val prop = buildProp(new FileInputStream(globalPropertiesFile))
    (
      prop.getProperty("output.dir") /: Dir,
      prop.getProperty("datasets.dir") /: Dir,
      prop.getProperty("lang"),
      prop.getProperty("dbpedia.version"))
  }

  protected def buildProp(is: InputStream) = {
    val prop = new Properties
    try {
      prop.load(is);
    } finally {
      is.close();
    }
    prop
  }

}