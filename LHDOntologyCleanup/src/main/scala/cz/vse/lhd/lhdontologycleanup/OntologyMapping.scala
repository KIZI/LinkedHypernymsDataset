package cz.vse.lhd.lhdontologycleanup

import java.io.File
import java.util.regex.Pattern

import scala.util.Try
import scala.xml.XML

/**
 * Created by propan on 18. 5. 2015.
 */
class OntologyMapping(ontology: File, lang: String) {

  private val nameClass = (XML.loadFile(ontology) \\ "Class")
    .filter(_.prefix == "owl")
    .flatMap { classElement =>
    val dbpediaClass = (classElement \ s"@{${classElement.getNamespace("rdf")}}about").text.trim
    val classLabels = (classElement \\ "label")
      .filter(labelElement => (labelElement \ s"@{${labelElement.getNamespace("xml")}}lang").text == lang)
      .map(_.text)
      .toSet
    val dbpediaClassName = dbpediaClass.replaceAll(".*/", "")
    (classLabels ++ Set(
      dbpediaClassName,
      dbpediaClassName.replaceAll("_", " "),
      dbpediaClassName.replaceAll("(?=\\p{Lu})", " ")
    )).map(_.trim.toLowerCase -> dbpediaClass)
  }.filter {
    case (x, y) => x.nonEmpty && y.nonEmpty
  }.toMap

  private def resourceName(resource: String) = resource.replaceAll(".*/", "").replaceAll("_", " ").trim.toLowerCase

  def mapResourceToOntology(resource: String) = nameClass.get(resourceName(resource))

  /**
   * returns the most precise subclass for argument
   * subclass is identified as class with longer name, containing the name of the class in the argument as substring
   * most precise = longest name, the subclass name must end with the argument
   * @param resource dbpedia resource uri
   * @return
   */
  def mapResourceToOntologySubclass(resource: String) = Try(
    nameClass.get(
      nameClass
        .keys
        .filter(_.matches(".+\\b" + Pattern.quote(resourceName(resource))))
        .maxBy(_.length)
    )
  ).getOrElse(None)

  /**
   * returns the most precise superclass for argument
   * superclass is identified as class with shorter name, contained in the name of the class in the argument as substring
   * most precise = longest of matching
   * @param resource dbpedia resource uri
   * @return
   */
  def mapResourceToOntologySuperclass(resource: String) = Try(
    nameClass.get {
      val resourceNorm = resourceName(resource)
      nameClass
        .keys
        .filter(ontologyClassName => resourceNorm.matches(".*" + Pattern.quote(ontologyClassName) + "s?") && resourceNorm.length > ontologyClassName.length)
        .maxBy(_.length)
    }
  ).getOrElse(None)

}
