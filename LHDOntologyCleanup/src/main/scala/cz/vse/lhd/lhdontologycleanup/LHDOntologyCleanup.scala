package cz.vse.lhd.lhdontologycleanup

import java.io.File

import cz.vse.lhd.core.BasicFunction._
import cz.vse.lhd.core.lucene.NTIndexer
import cz.vse.lhd.core.{AppConf, NTReader, RdfTriple}
import cz.vse.lhd.lhdontologycleanup.output._
import org.apache.commons.io.FileUtils

import scala.collection.mutable.ListBuffer

object LHDOntologyCleanup extends AppConf {

  AppConf.args match {
    case Array(_, "index") => index()
    case _ =>
      makeOutputFiles()
  }

  def index() = {
    val indexDir = new File(Conf.indexDir)
    FileUtils.cleanDirectory(indexDir)
    tryClose(new NTIndexer(indexDir)) { indexer =>
      if (Conf.lang != "en") NTReader.fromFile(new File(Conf.datasetInterlanguage_linksEnPath)) { it =>
        indexer.index(it
          .map(stmt => RdfTriple(stmt.getObject.asResource().getURI, stmt.getPredicate.getURI, stmt.getSubject.getURI))
          .filter(_.subject.contains("http://" + Conf.lang))
        )
      }
      for (file <- Set(
        Conf.datasetInstance_typesEnPath,
        Conf.datasetInstance_typesPath,
        Conf.datasetInstance_typesTransitiveEnPath,
        Conf.datasetInstance_typesTransitivePath
      ).map(filePath => new File(filePath))) NTReader.fromFile(file) { it =>
        indexer.index(it
          .map(stmt => RdfTriple(stmt))
        )
      }
    }
  }

  private def outputNameToFile(file: String) = new File(Conf.outputDir + file)

  private def runPipeline(pipeline: Seq[(String, OutputMaker)]) = for ((outputFile, maker) <- pipeline) {
    OutputMaker.process(outputNameToFile(outputFile), maker)
  }

  def makeOutputFiles() = {
    val pipeline = ListBuffer.empty[(String, OutputMaker)]
    val manualMapping = new ManualMapping(Conf.manualmappingOverridetypesPath, Conf.manualmappingExcludetypesPath)
    val ontologyMapping = Set(Conf.lang, "en").map(lang => lang -> new SingleOntologyMapping(new File(Conf.datasetOntologyPath), lang)).toMap
    pipeline += (Conf.Output.hypoutDbpediaUnique -> new UniqueLinesOutput(outputNameToFile(Conf.Output.hypoutDbpedia)))
    val nextInput = outputNameToFile(
      if (Conf.lang != "en") {
        pipeline += (Conf.Output.hypoutEnAligned -> new EnAlignedOutput(outputNameToFile(Conf.Output.hypoutTypeOverride)))
        Conf.Output.hypoutEnAligned
      } else {
        Conf.Output.hypoutDbpediaUnique
      }
    )
    pipeline += (Conf.Output.classEquivallence -> new ClassEquivallenceOutput(nextInput, ontologyMapping))
    pipeline += (Conf.Output.classSubclass -> new ClassSubclassOutput(nextInput, ontologyMapping))
    pipeline += (Conf.Output.classSuperclass -> new ClassSuperclassOutput(nextInput, ontologyMapping))
    pipeline += (Conf.Output.hypoutTypeOverride -> new TypeOverrideOutput(nextInput, manualMapping))
    pipeline += (Conf.Output.hypoutManualExclusion -> new ManualExclusionOutput(outputNameToFile(Conf.Output.hypoutTypeOverride), manualMapping))
    val inputFile = outputNameToFile(Conf.Output.hypoutManualExclusion)
    pipeline += (Conf.Output.instancesMapped -> new MappedOutput(inputFile, ontologyMapping))
    pipeline += (Conf.Output.instancesNotMapped -> new NotMappedOutput(inputFile, ontologyMapping))
    pipeline += (Conf.Output.instancesNotMappedSuperMapped -> new NotMappedSuperMappedOutput(
      outputNameToFile(Conf.Output.instancesMapped),
      outputNameToFile(Conf.Output.instancesNotMapped)
    ))
    runPipeline(pipeline)
  }

}