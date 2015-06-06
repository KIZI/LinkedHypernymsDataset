package cz.vse.lhd.ontologycleanup.output

import java.io.{File, OutputStream}

import cz.vse.lhd.core.{NTReader, NTWriter, RdfTriple}
import cz.vse.lhd.ontologycleanup.{LanguageMapping, OntologyMapping}

/**
 * Created by propan on 18. 5. 2015.
 */
class ClassSuperclassOutput(input: File, langOntologyMapping: Map[String, OntologyMapping]) extends OutputMaker with OutputMakerHeader {

  val header: String = "# The mappings from DBpedia article to its superclass - a DBpedia ontology class\n# These are most likely all wrong, since the confirmed mappings from *.class.superclass.confirmed.nt do not appear in this file"

  def makeFile(output: OutputStream) = NTReader.fromFile(input) { it =>
    NTWriter.fromIterator(
      for {
        hypernym <- it.map(_.getObject.asResource().getURI).toSet[String].toIterator
        dbo <- langOntologyMapping(LanguageMapping.langByResource(hypernym)).mapResourceToOntologySuperclass(hypernym)
      } yield {
        RdfTriple(hypernym, "http://www.w3.org/2000/01/rdf-schema#subClassOf", dbo)
      },
      output
    )
  }

}
