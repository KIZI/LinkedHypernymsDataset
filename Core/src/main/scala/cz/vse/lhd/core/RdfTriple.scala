package cz.vse.lhd.core

import com.hp.hpl.jena.rdf.model.{ModelFactory, Statement}
import cz.vse.lhd.core.RdfTriple.{Literal, Resource, ObjectType}

/**
 * Created by propan on 15. 5. 2015.
 */
case class RdfTriple(subject: String, predicate: String, `object`: String) {
  def toStatement(implicit objectType: ObjectType = Resource) = {
    val model = ModelFactory.createDefaultModel
    model.createStatement(
      model.createResource(subject),
      model.createProperty(predicate),
      objectType match {
        case Resource => model.createResource(`object`)
        case Literal(lang) => model.createLiteral(`object`, lang)
      }
    )
  }
}

object RdfTriple {
  def apply(stmt: Statement): RdfTriple = RdfTriple(stmt.getSubject.getURI, stmt.getPredicate.getURI, stmt.getObject.toString)

  sealed trait ObjectType

  case class Literal(lang: String) extends ObjectType

  object Resource extends ObjectType

}
