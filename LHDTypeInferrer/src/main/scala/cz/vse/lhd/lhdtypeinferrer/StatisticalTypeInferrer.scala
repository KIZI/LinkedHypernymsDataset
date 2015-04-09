package cz.vse.lhd.lhdtypeinferrer

import com.hp.hpl.jena.rdf.model.Statement

import scala.util.Try

/**
 * Created by propan on 8. 4. 2015.
 */
object StatisticalTypeInferrer {

  trait ResultProcessor {
    def process(hypernym: String, hypos: List[String], inferredType: String, confidence: Float)
  }

  def inferTypes(input: Iterator[Statement], rp: ResultProcessor)(implicit tr: TypeReader, oc: OntologyChecker) = {
    //load all triples from dataset to Map where the key is some hypernym and values are all subjects (=hypos) having the hypernym
    //then start to iterate of this Map
    for (
      (hyp, hypos) <- input.foldLeft(Map.empty[String, List[String]]) { (hypMap, stmt) =>
        val sub = stmt.getSubject.getURI
        val obj = stmt.getObject.asResource().getURI
        hypMap + (obj -> (sub :: hypMap.getOrElse(obj, Nil)))
      }
    ) {
      //load all dbpedia types of hypos (from instance_types dataset) and filter right dbpedia types
      //then for each dbpedia type we count the number of occurrences and return Map where the key is some type and the value is some number of occurs
      val typeFrequency = hypos.foldLeft(Map.empty[String, Int]) { (typeMap, hypo) =>
        tr.getInstanceTypes(hypo).filter(oc.isType).foldLeft(typeMap) { (typeMap, instType) =>
          typeMap + (instType -> (typeMap.getOrElse(instType, 0) + 1))
        }
      }
      //count the total number of occurrences of all dbpedia types for all hypos
      val totalFreq = typeFrequency.values.sum
      //filter dbpedia types which do not have any subType OR their subTypes have a low support
      //then choose a dbpedia type with the maximum frequency
      //if the best dbpedia type exists, save it as a value
      for (
        (bestType, bestFreq) <- Try(
          typeFrequency.filter {
            case (superType, superFreq) =>
              val minSubTypeFrequency = superFreq * 0.2F
              !typeFrequency.exists {
                case (subType, subFreq) => oc.isTransitiveSubtype(superType, subType) && subFreq > minSubTypeFrequency
              }
          }.maxBy {
            case (_, count) => count
          }
        )
      ) {
        //count the confidence of the chosen dbpedia type
        val confidence = bestFreq.toFloat / totalFreq
        //process the result of one hypernym
        rp.process(hyp, hypos, bestType, confidence)
      }
    }

  }

}
