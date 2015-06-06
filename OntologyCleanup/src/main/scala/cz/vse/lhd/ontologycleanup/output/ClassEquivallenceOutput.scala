package cz.vse.lhd.ontologycleanup.output

import java.io.{File, OutputStream}

import cz.vse.lhd.core.{RdfTriple, NTWriter, NTReader}
import cz.vse.lhd.ontologycleanup.{LanguageMapping, OntologyMapping}

/**
 * Created by propan on 18. 5. 2015.
 */
class ClassEquivallenceOutput(input: File, langOntologyMapping: Map[String, OntologyMapping]) extends OutputMaker with OutputMakerHeader {

  val header: String = "# Sameas mappings from DBpedia article to DBpedia ontology"

  def makeFile(output: OutputStream) = NTReader.fromFile(input) { it =>
    NTWriter.fromIterator(
      for {
        hypernym <- it.map(_.getObject.asResource().getURI).toSet[String].toIterator
        dbo <- langOntologyMapping(LanguageMapping.langByResource(hypernym)).mapResourceToOntology(hypernym)
      } yield {
        RdfTriple(hypernym, "http://www.w3.org/2002/07/owl#sameAs", dbo)
      },
      output
    )
  }

}
