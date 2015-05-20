package cz.vse.lhd.lhdontologycleanup.output

import java.io.{File, OutputStream}

import cz.vse.lhd.core.{NTReader, NTWriter, RdfTriple}
import cz.vse.lhd.lhdontologycleanup.{LanguageMapping, OntologyMapping}

/**
 * Created by propan on 18. 5. 2015.
 */
class MappedOutput(input: File, langOntologyMapping: Map[String, OntologyMapping]) extends OutputMaker with OutputMakerHeader {

  val header: String = "# Mapped hypernyms to DBpedia ontology"

  def makeFile(output: OutputStream) = NTReader.fromFile(input) { it =>
    NTWriter.fromIterator(
      for {
        stmt <- it
        hypernym = stmt.getObject.asResource().getURI
        dbo <- langOntologyMapping(LanguageMapping.langByResource(hypernym)).mapResourceToOntology(hypernym)
      } yield {
        RdfTriple(stmt.getSubject.getURI, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", dbo)
      },
      output
    )
  }

}