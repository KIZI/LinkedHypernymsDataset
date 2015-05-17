package cz.vse.lhd.lhdontologycleanup

import java.io.File

import cz.vse.lhd.core.BasicFunction._
import cz.vse.lhd.core.lucene.NTIndexer
import cz.vse.lhd.core.{AppConf, BasicFunction, NTReader, RdfTriple}
import org.apache.commons.io.FileUtils

object LHDOntologyCleanup extends AppConf {

  //THDOntologyCleanup.run(AppConf.args)

  //  def makeUniqueHypoutOutput() = {
  //    OutputHeader.apply(new FileOutputStream(Conf.outputDir + Conf.Output.hypoutDbpediaUnique), "# Input file with duplicate lines removed") { os =>
  //      UniqueLinesOutput(new FileInputStream(Conf.outputDir + Conf.Output.hypoutDbpedia), os)
  //    }
  //  }
  //
  //  def makeTypeOverrideOutput(manualMapping: ManualMapping) = {
  //
  //  }

  //  val indexDir = new File(Conf.indexDir)
  //  FileUtils.cleanDirectory(indexDir)
  //  tryClose(new NTIndexer(indexDir)) { indexer =>
  //    if (Conf.lang != "en") NTReader.fromFile(new File(Conf.datasetInterlanguage_linksEnPath)) { it =>
  //      indexer.index(it
  //        .map(stmt => RdfTriple(stmt.getObject.asResource().getURI, stmt.getPredicate.getURI, stmt.getSubject.getURI))
  //        .filter(_.subject.contains("http://" + Conf.lang))
  //      )
  //    }
  //    for (file <- Set(
  //      Conf.datasetInstance_typesEnPath,
  //      Conf.datasetInstance_typesPath,
  //      Conf.datasetInstance_typesTransitiveEnPath,
  //      Conf.datasetInstance_typesTransitivePath
  //    ).map(filePath => new File(filePath))) NTReader.fromFile(file) { it =>
  //      indexer.index(it
  //        .map(stmt => RdfTriple(stmt))
  //      )
  //    }
  //  }

  tryClose(new NTIndexer(Conf.indexDir)) { indexer =>
    indexer.search { searcher =>
      println(searcher("http://dbpedia.org/resource/Revive_(Bjørn_Lynne_album)"))
    }
  }

}
