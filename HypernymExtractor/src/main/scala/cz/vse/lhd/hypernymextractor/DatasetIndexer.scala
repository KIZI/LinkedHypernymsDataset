package cz.vse.lhd.hypernymextractor

import java.io.File

import com.hp.hpl.jena.query.ARQ
import com.hp.hpl.jena.rdf.model.Statement
import cz.vse.lhd.core.BasicFunction._
import cz.vse.lhd.core.lucene.NTIndexer
import cz.vse.lhd.core.{AppConf, NTReader}
import org.apache.commons.io.FileUtils

object DatasetIndexer {

  ARQ.init()

  def index(indexDir: File, datasetDisambiguations: File, datasetShort_abstracts: File) = {
    if (!indexDir.isDirectory)
      indexDir.mkdirs()
    FileUtils.cleanDirectory(indexDir)
    NTReader.fromFile(new File(Conf.datasetDisambiguations)) {
      it =>
        implicit val disambiguations = it.map(_.getSubject.getURI).toSet
        NTReader.fromFile(new File(Conf.datasetShort_abstractsPath)) {
          absIt =>
            tryClose(new NTIndexer(indexDir))(_.index(absIt.filter(filterDisambiguation)))
        }
    }
  }

  private def filterDisambiguation(stmt: Statement)(implicit disambiguations: Set[String]) = !disambiguations(stmt.getSubject.getURI)

}
