package cz.vse.lhd.lhdontologycleanup.output

import java.io.{File, OutputStream}

import cz.vse.lhd.core.{RdfTriple, NTReader, NTWriter}
import cz.vse.lhd.lhdontologycleanup.{LanguageMapping, OntologyMapping}

/**
 * Created by propan on 18. 5. 2015.
 */
class ClassSubclassOutput(input: File, langOntologyMapping: Map[String, OntologyMapping]) extends OutputMaker with OutputMakerHeader {

  val header: String = "# The mappings from DBpedia article to its subclass - a DBpedia ontology class"

  def makeFile(output: OutputStream) = NTReader.fromFile(input) { it =>
    NTWriter.fromIterator(
      for {
        hypernym <- it.map(_.getObject.asResource().getURI).toSet.toIterator
        dbo <- langOntologyMapping(LanguageMapping.langByResource(hypernym)).mapResourceToOntologySubclass(hypernym)
      } yield {
        RdfTriple(dbo, "http://www.w3.org/2000/01/rdf-schema#subClassOf", hypernym)
      },
      output
    )
  }

}
