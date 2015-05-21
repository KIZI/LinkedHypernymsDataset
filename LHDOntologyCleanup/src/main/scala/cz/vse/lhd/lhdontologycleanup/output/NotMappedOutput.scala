package cz.vse.lhd.lhdontologycleanup.output

import java.io.{File, OutputStream}

import cz.vse.lhd.core.{NTReader, NTWriter}
import cz.vse.lhd.lhdontologycleanup.{LanguageMapping, OntologyMapping}

/**
 * Created by propan on 18. 5. 2015.
 */
class NotMappedOutput(input: File, langOntologyMapping: Map[String, OntologyMapping]) extends OutputMaker with OutputMakerHeader {

  val header: String = "# Hypernyms which failed to map to DBpedia ontology"

  def makeFile(output: OutputStream) = NTReader.fromFile(input) { it =>
    NTWriter.fromIterator(
      for {
        stmt <- it
        hypernym = stmt.getObject.asResource().getURI
        if langOntologyMapping(LanguageMapping.langByResource(hypernym)).mapResourceToOntology(hypernym).isEmpty
      } yield {
        stmt
      },
      output
    )
  }

}