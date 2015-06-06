package cz.vse.lhd.typeinferrer.impl

import java.io.File

import cz.vse.lhd.core.BasicFunction._
import cz.vse.lhd.core.{RdfTriple, NTReader}
import cz.vse.lhd.core.lucene.NTIndexer
import cz.vse.lhd.typeinferrer.{Conf, TypeReader}
import org.apache.commons.io.FileUtils

/**
 * Created by propan on 26. 5. 2015.
 */
class LuceneTypeReader private(searcher: String => Seq[RdfTriple]) extends TypeReader {

  def isInstance(instance: String): Boolean = searcher(instance).nonEmpty

  def isInstanceType(instance: String, instanceType: String): Boolean = searcher(instance).exists(_.`object` == instanceType)

  def getInstanceTypes(instance: String): Seq[String] = searcher(instance).map(_.`object`)

}

object LuceneTypeReader {

  def index() = {
    val indexDir = new File(Conf.indexDir)
    FileUtils.cleanDirectory(indexDir)
    tryClose(new NTIndexer(indexDir)) { indexer =>
      for (file <- Set(
        Conf.datasetInstance_typesPath,
        Conf.datasetInstance_typesTransitivePath,
        Conf.datasetInstance_typesEnPath,
        Conf.datasetInstance_typesTransitiveEnPath
      ).map(filePath => new File(filePath))) NTReader.fromFile(file) { it =>
        indexer.index(it
          .map(stmt => RdfTriple(stmt))
        )
      }
    }
  }

  def apply(read: LuceneTypeReader => Unit) = tryClose(new NTIndexer(Conf.indexDir)) { indexer =>
    indexer.search { searcher =>
      read(new LuceneTypeReader(searcher))
    }
  }

}
