package cz.vse.lhd.lhdontologycleanup.output

import java.io.{File, OutputStream}

import cz.vse.lhd.core.{NTWriter, NTReader}
import cz.vse.lhd.lhdontologycleanup.OntologyMapping

/**
 * Created by propan on 18. 5. 2015.
 */
class ClassEquivallenceOutput(input: File, ontologyMapping: OntologyMapping) extends OutputMaker with OutputMakerHeader {

  val header: String = "# Sameas mappings from DBpedia article to DBpedia ontology"

  def makeFile(output: OutputStream) = NTReader.fromFile(input) { it =>
    NTWriter.fromIterator(
      for {
        stmt <- it
        dbo <- ontologyMapping.mapResourceToOntology(stmt.getObject.asResource().getURI)
      } yield {
        stmt.changeObject(dbo)
      },
      output
    )
  }

}
