package cz.vse.lhd.core

import java.io.File

import com.github.kxbmap.configs._
import com.typesafe.config.ConfigFactory

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

  protected lazy val config = new EnrichTypesafeConfig(ConfigFactory.parseFile(new File(globalPropertiesFile)))

  lazy val (
    outputDir,
    datasetsDir,
    lang,
    dbpediaVersion
    ) = {
    (
      config.get[String]("LHD.output.dir") /: Dir,
      config.get[String]("LHD.datasets.dir") /: Dir,
      config.get[String]("LHD.lang"),
      config.get[String]("LHD.dbpedia.version")
    )
  }

  lazy val dbpediaBasicUri = "http://" + (if (lang == "en") "" else s"$lang.") + "dbpedia.org/"
  lazy val hypernymPredicate = "http://purl.org/linguistics/gold/hypernym"

  object Output {
    val hypoutName = "hypoutput"
    val hypoutRawSuffix = "raw"
    val hypoutDbpediaSuffix = "dbpedia"
    val hypoutLogSuffix = "log"
    val hypoutRaw = s"$hypoutName.$hypoutLogSuffix.$hypoutRawSuffix"
    val hypoutDbpedia = s"$hypoutName.$hypoutLogSuffix.$hypoutDbpediaSuffix"
    val hypoutDbpediaUnique = s"$lang.$hypoutDbpedia.unique.nt"
    val hypoutTypeOverride = s"$lang.$hypoutDbpedia.typeoverride.nt"
  }

}