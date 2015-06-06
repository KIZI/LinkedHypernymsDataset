package cz.vse.lhd.tasks

import java.io.{File, FileOutputStream}
import java.nio.file.{StandardCopyOption, Files}

import cz.vse.lhd.core.BasicFunction._
import cz.vse.lhd.core.{NTReader, NTWriter, RdfTriple}
import cz.vse.lhd.{Conf, Task}

import scala.io.Source

/**
 * Created by propan on 6. 6. 2015.
 */
class FinalDatasetsMakingTask extends Task {

  def run(): Unit = {
    tryClose(Source.fromFile(Conf.outputDir + Conf.Output.hypoutRaw)) { source =>
      val RawTuple = "(.+);(.+)".r
      val langPrefix = if (Conf.lang != "en") Conf.lang + "." else ""
      val it = source.getLines().collect {
        case RawTuple(resource, hypernym) => RdfTriple(s"http://${langPrefix}dbpedia.org/$resource", "http://purl.org/linguistics/gold/hypernym", hypernym)
      }
      NTWriter.fromIterator(it, new FileOutputStream(Conf.outputDir + Conf.Output.finalRaw))(RdfTriple.Literal(Conf.lang))
    }
    Files.copy(new File(Conf.outputDir + Conf.Output.hypoutDbpediaUnique).toPath, new File(Conf.outputDir + Conf.Output.finalExtension).toPath, StandardCopyOption.REPLACE_EXISTING)
    NTReader.fromFile(new File(Conf.outputDir + Conf.Output.instancesMapped)) { it1 =>
      NTReader.fromFile(new File(Conf.outputDir + Conf.Output.instancesNotMappedSuperMapped)) { it2 =>
        NTWriter.fromIterator(it1 ++ it2, new FileOutputStream(Conf.outputDir + Conf.Output.finalCore))
      }
    }
    NTReader.fromFile(new File(Conf.outputDir + Conf.Output.inferredMappingsToDbpedia)) { itHyp =>
      val hypernymToOntologyType = itHyp.map(stmt => stmt.getSubject.getURI -> stmt.getObject.asResource().getURI).toMap
      NTReader.fromFile(new File(Conf.outputDir + Conf.Output.hypoutManualExclusion)) { itRes =>
        NTWriter.fromIterator(
          for {
            stmt <- itRes
            triple = RdfTriple(stmt)
            ontologyType = hypernymToOntologyType.get(triple.`object`)
            if ontologyType.isDefined
          } yield {
            triple.copy(predicate = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", `object` = ontologyType.get)
          },
          new FileOutputStream(Conf.outputDir + Conf.Output.inferredMapping)
        )
      }
    }
    Files.copy(new File(Conf.outputDir + Conf.Output.inferredMapping).toPath, new File(Conf.outputDir + Conf.Output.finalInference).toPath, StandardCopyOption.REPLACE_EXISTING)
  }

}
