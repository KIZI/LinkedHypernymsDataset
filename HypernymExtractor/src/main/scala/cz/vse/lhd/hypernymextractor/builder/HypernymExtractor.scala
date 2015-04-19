package cz.vse.lhd.hypernymextractor.builder

import java.io.File

import cz.vse.lhd.hypernymextractor.builder.HypernymExtractor._
import cz.vse.lhd.hypernymextractor.{Conf, ProcessStatus}
import gate.creole.SerialAnalyserController
import gate.{Corpus, Factory, ProcessingResource}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

/**
 * Created by propan on 17. 4. 2015.
 */
class HypernymExtractor private (dbpediaLinker: DBpediaLinker, private var processStatus: ProcessStatus) {

  private val lang = Conf.lang
  private lazy val logger = LoggerFactory.getLogger(getClass)

  private lazy val pipeline = {
    val pipeline = Factory.createResource("gate.creole.RealtimeCorpusController").asInstanceOf[SerialAnalyserController]
    val resetPR = Factory.createResource("gate.creole.annotdelete.AnnotationDeletePR", Factory.newFeatureMap()).asInstanceOf[ProcessingResource]
    val tokenizerPR = Factory.createResource("gate.creole.tokeniser.DefaultTokeniser", Factory.newFeatureMap()).asInstanceOf[ProcessingResource]
    pipeline.add(resetPR)
    pipeline.add(tokenizerPR)

    if (lang.equals("en")) {
      val sentenceSplitterPR = Factory.createResource("gate.creole.splitter.RegexSentenceSplitter", Factory.newFeatureMap()).asInstanceOf[ProcessingResource]
      val posTaggerPR = Factory.createResource("gate.creole.POSTagger", Factory.newFeatureMap()).asInstanceOf[ProcessingResource]
      pipeline.add(sentenceSplitterPR)
      pipeline.add(posTaggerPR)
    } else if (lang.equals("nl") || lang.equals("de")) {
      val taggerFeatureMap = Factory.newFeatureMap()
      taggerFeatureMap.put("debug", "false")
      taggerFeatureMap.put("encoding", "utf-8")
      //                if (!lang.equals("de")) {
      //                    taggerFeatureMap.put("encoding", "utf-8");
      //                } else {
      //                    // for de, utf-8 causes on some documents tagger to fail
      //                    taggerFeatureMap.put("encoding", "ISO-8859-1");
      //                }
      taggerFeatureMap.put("failOnUnmappableCharacter", "false")
      taggerFeatureMap.put("featureMapping", "lemma=3;category=2;string=1")
      taggerFeatureMap.put("inputAnnotationType", "Token")
      taggerFeatureMap.put("inputTemplate", "${string}")
      taggerFeatureMap.put("outputAnnotationType", "Token")
      taggerFeatureMap.put("regex", "(.+)	(.+)	(.+)")
      if (lang.equals("de")) {
        taggerFeatureMap.put("taggerBinary", "resources/TreeTagger/tree-tagger-german-gate")
      } else if (lang.equals("nl")) {
        taggerFeatureMap.put("taggerBinary", "resources/TreeTagger/tree-tagger-dutch-gate")
      }
      val genTag = Factory.createResource("gate.taggerframework.GenericTagger", taggerFeatureMap).asInstanceOf[ProcessingResource]
      pipeline.add(genTag)
    }
    val transducerFeatureMap = Factory.newFeatureMap()
    transducerFeatureMap.put("grammarURL", new File(Conf.gateJapeGrammar).toURI.toURL)
    transducerFeatureMap.put("encoding", "UTF-8")
    val japeCandidatesPR = Factory.createResource("gate.creole.Transducer", transducerFeatureMap).asInstanceOf[ProcessingResource]
    pipeline.add(japeCandidatesPR)
    pipeline
  }

  def extractHypernyms(corpus: Corpus)(implicit processHypernym: Hypernym => Unit) = {
    logger.debug(s"Corpus pipeline executing with size: ${corpus.size()}")
    pipeline.setCorpus(corpus)
    pipeline.execute()
    for (doc <- corpus.iterator().asScala) {
      processStatus.tryPrint()
      processStatus = processStatus.plusplus
      for (isaAnnot <- doc.getAnnotations.get("h").iterator().asScala) {
        val hypernym = {
          val hypernym = Option(isaAnnot.getFeatures)
            .filter(_.containsKey("lemma"))
            .map(_.get("lemma"))
            .filter(_ != null)
            .map(_.toString)
            .filter(x => x.length > 0 && x != "<unknown>")
            .getOrElse(doc.getContent.getContent(isaAnnot.getStartNode.getOffset, isaAnnot.getEndNode.getOffset).toString)
          Hypernym(
            doc.getFeatures.get("article_title").asInstanceOf[String],
            doc.getFeatures.get("dbpedia_url").asInstanceOf[String],
            hypernym,
            dbpediaLinker.getLink(hypernym)
          )
        }
        logger.debug(s"Hypernym extracted: $hypernym")
        processHypernym(hypernym)
      }
    }
  }

  /*  try {} catch {
      case e @ (_: InvalidOffsetException | _: ExecutionException) => logger.error(e.getMessage, e)
    }*/

}

object HypernymExtractor {

  case class Hypernym(resourceName: String, resourceUri: String, rawHypernym: String, resourceHypernym: Option[String])

  def apply(dbpediaLinker: DBpediaLinker, start: Int, end: Int)(extractAll: HypernymExtractor => Unit) = {
    extractAll(new HypernymExtractor(dbpediaLinker, new ProcessStatus(end - start)))
  }

}