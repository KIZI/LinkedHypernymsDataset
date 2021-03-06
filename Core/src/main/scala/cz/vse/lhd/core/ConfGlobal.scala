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

  def args = _args

}

trait ConfGlobal {

  val globalPropertiesFile: String

  protected lazy val config = new EnrichTypesafeConfig(ConfigFactory.parseFile(new File(globalPropertiesFile)).resolve())

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

  val hypernymPredicate = "http://purl.org/linguistics/gold/hypernym"
  val dbpediaOntologyUri = "http://dbpedia.org/ontology/"
  val dbpediaResourceUriRegexp = "http://(.+?\\.)?dbpedia.org/resource/"

  object Output {
    val hypoutName = "hypoutput"
    val hypoutRawSuffix = "raw"
    val hypoutDbpediaSuffix = "dbpedia"
    val hypoutLogSuffix = "log"
    val hypoutRaw = s"$hypoutName.$hypoutLogSuffix.$hypoutRawSuffix"
    val hypoutDbpedia = s"$hypoutName.$hypoutLogSuffix.$hypoutDbpediaSuffix"
    val hypoutDbpediaUnique = s"$lang.$hypoutDbpedia.unique.nt"
    val hypoutTypeOverride = s"$lang.$hypoutDbpedia.typeoverride.nt"
    val hypoutEnAligned = s"$lang.$hypoutDbpedia.en-aligned.nt"
    val hypoutManualExclusion = s"$lang.$hypoutDbpedia.manualexclusion.nt"
    val classEquivallence = s"$lang.class.equivallence.nt"
    val classSuperclass = s"$lang.class.superclass.nt"
    val classSubclass = s"$lang.class.subclass.nt"
    val instancesMapped = s"$lang.instances.mapped.nt"
    val instancesNotMapped = s"$lang.instances.notmapped.nt"
    val instancesNotMappedSuperMapped = s"$lang.instances.notmapped.mappedsupertype.nt"
    val inferredMappingsToDbpedia = s"$lang.inferredmappingstodbpedia.nt"
    val inferredMapping = s"$lang.inferredmapping.nt"
    val finalCore = s"$lang.lhd.core.$dbpediaVersion.nt"
    val finalInference = s"$lang.lhd.inference.$dbpediaVersion.nt"
    val finalExtension = s"$lang.lhd.extension.$dbpediaVersion.nt"
    val finalRaw = s"$lang.lhd.raw.$dbpediaVersion.nt"
  }

}