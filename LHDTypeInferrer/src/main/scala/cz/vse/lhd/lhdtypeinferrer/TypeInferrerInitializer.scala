package cz.vse.lhd.lhdtypeinferrer

import com.hp.hpl.jena.query.ARQ
import cz.vse.lhd.core.AppConf
import cz.vse.lhd.core.FileExtractor
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.PrintWriter
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream

object TypeInferrerInitializer extends AppConf {
  
  import scala.collection.JavaConversions._
  ARQ.init
  
  FileProcessor(
    List(
      "instances.less_sure.nt",
      "instances.mapped.existing.nt",
      "instances.mapped.new.notypebefore.nt",
      "instances.mapped.new.nt",
      "instances.notmapped.new.nt",
      "instances.notmapped.probablyexisting.nt",
      "instances.notmapped.suspicious.nt"
    ) map toFilePath collect toFile,
    List(
      "instances.all.nt",
      "LHDv1.draft.nt"
    ) map toFilePath
  ) foreach (x => {
      val statsFile = new PrintWriter(new FileOutputStream(toFilePath("instances.all.stat")))
      try {
        x
        .enableStats(Map.empty)
        .process
        .stats
        .toList
        .sortWith{
          case ((_, n1), (_, n2)) => n1 > n2  
        } foreach {
          case (dbt, x) => statsFile.println(s"$x $dbt")
        }
      } finally {
        statsFile.close
      }
      THDTypeInferrer.run(Array())
    }
  )
  
  FileProcessor(
    List("instances.all.inferredmapping.nt") map toFilePath collect toFile,
    List("LHDv2.draft.nt") map toFilePath
  ) foreach (
    _
    .filter(_.getObject.asResource.getURI.startsWith("http://dbpedia.org/ontology"))
    .process
  )
  
  val files = new File(Conf.outputDir).listFiles filter (file => file.isFile && !file.getName.endsWith("draft.nt") && !file.getName.endsWith("draft.zip"))
  if (Conf.compressTemporaryFiles && !files.isEmpty) {
    val zos = new ZipArchiveOutputStream(new File(toFilePath("temp.draft.zip")))
    for (file <- files) {
      val source = new BufferedInputStream(new FileInputStream(file))
      zos.putArchiveEntry(new ZipArchiveEntry(file, file.getName))
      Stream continually (source.read) takeWhile (_ != -1) foreach zos.write
      source.close
      zos.closeArchiveEntry
      file.delete
    }
    zos.close
  }
  
  def toFilePath(fName : String) = Conf.outputDir + Conf.lang + "." + fName
  def toFile : PartialFunction[String, File] = {
    case FileExtractor(f) => f
  }
  
}