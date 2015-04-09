package cz.vse.lhd.lhdtypeinferrer.impl

import java.io.{StringWriter, FileInputStream, File}

import com.hp.hpl.jena.rdf.model.ModelFactory
import cz.vse.lhd.core.NTReader
import cz.vse.lhd.lhdtypeinferrer.OntologyChecker

import scala.io.Source

/**
 * Created by propan on 9. 4. 2015.
 */
class OwlOntologyChecker(owl: File) extends OntologyChecker {

  val classes = collection.mutable.Map.empty[String, collection.mutable.ListBuffer[String]]

  {
    //load rdf/xml owl and tranform it to the n-triple format
    val model = ModelFactory.createDefaultModel()
    model.read(new FileInputStream(owl), null, "RDF/XML")
    val n3 = new StringWriter()
    model.write(n3, "N-TRIPLE")
    n3.flush()
    //load the n-triple owl dataset and read only triples which have the rdfs:subClassOf predicate
    //triples of the class taxonomy are saved into Map where the key is some class and values are superclasses of the class
    for (
      stmt <- NTReader.fromSource(Source.fromString(n3.toString))
      if List("rdfs:subClassOf", "http://www.w3.org/2000/01/rdf-schema#subClassOf") exists stmt.getPredicate.getURI.contains
    )
      classes.getOrElseUpdate(stmt.getSubject.getURI, collection.mutable.ListBuffer.empty) += stmt.getObject.asResource().getURI
  }

  def isType(name: String): Boolean = classes.contains(name)

  def isTransitiveSubtype(superType: String, subType: String): Boolean = classes.get(subType).exists(
    _.exists(
      superSubType =>
        superSubType == superType || isTransitiveSubtype(superType, superSubType)
    )
  )

}
