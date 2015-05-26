package cz.vse.lhd.lhdtypeinferrer

import java.io.{File, FileOutputStream}

import com.hp.hpl.jena.rdf.model.Statement
import cz.vse.lhd.core._
import cz.vse.lhd.lhdtypeinferrer.impl.{FileSTIDebugger, LuceneTypeReader, OwlOntologyChecker, SimpleHypernymDebugger}

import scala.util.Try

/**
 * Created by propan on 8. 4. 2015.
 */
object StatisticalTypeInferrer extends AppConf {

  AppConf.args match {
    case Array(_, "index") => LuceneTypeReader.index()
    case _ => LuceneTypeReader { typeReader =>
      val ontologyChecker = new OwlOntologyChecker(new File(Conf.datasetOntologyPath))
      FileSTIDebugger(new File(Conf.outputDir + Conf.lang + ".sti.debug")) { stiDebugger =>
        NTWriter.write(new FileOutputStream(Conf.outputDir + Conf.Output.inferredMappingsToDbpedia)) { writer =>
          val resultProcessor = new ResultProcessor {
            def process(hypernym: String, inferredType: String): Unit = {
              writer(RdfTriple(hypernym, "http://www.w3.org/2000/01/rdf-schema#subClassOf", inferredType).toStatement)
            }
          }
          NTReader.fromFile(new File(Conf.outputDir + Conf.Output.hypoutManualExclusion)) { it =>
            inferTypes(it, resultProcessor, stiDebugger)(typeReader, ontologyChecker)
          }
        }
      }
    }
  }

  trait ResultProcessor {
    def process(hypernym: String, inferredType: String)
  }

  def inferTypes(input: Iterator[Statement], rp: ResultProcessor, debugger: STIDebugger)(implicit tr: TypeReader, oc: OntologyChecker) = {
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
      val prunedTypeFrequency = typeFrequency.filter {
        case (superType, superFreq) =>
          val minSubTypeFrequency = superFreq * 0.2F
          !typeFrequency.exists {
            case (subType, subFreq) => oc.isTransitiveSubtype(superType, subType) && subFreq > minSubTypeFrequency
          }
      }
      //then choose a dbpedia type with the maximum frequency
      //if the best dbpedia type exists, save it as a value
      for (
        (bestType, bestFreq) <- Try(
          prunedTypeFrequency.maxBy {
            case (_, count) => count
          }
        ).toOption
      ) {
        //count the confidence of the chosen dbpedia type
        val confidence = bestFreq.toFloat / totalFreq
        //debug
        debugger.debug(
          new SimpleHypernymDebugger(hyp)
            .allCandidates(typeFrequency, totalFreq)
            .prunedCandidates(prunedTypeFrequency, totalFreq)
            .selectedType(bestType, confidence)
        )
        //process the result of one hypernym
        rp.process(hyp, bestType)
      }
    }

  }

}
