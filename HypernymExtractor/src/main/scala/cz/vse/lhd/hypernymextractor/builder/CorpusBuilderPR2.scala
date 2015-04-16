package cz.vse.lhd.hypernymextractor.builder

import java.io.File

import com.hp.hpl.jena.query.ARQ
import com.hp.hpl.jena.rdf.model.Statement
import cz.vse.lhd.core.{BasicFunction, NTReader}
import cz.vse.lhd.hypernymextractor.Conf
import gate.{Corpus, Factory, Gate, ProcessingResource}
import gate.creole.SerialAnalyserController
import org.slf4j.LoggerFactory

import scala.io.Source

class CorpusBuilderPR2 extends CorpusBuilderPR {

  lazy val logger = LoggerFactory.getLogger(getClass)

  val pipeline = {
    val pl = Factory.createResource("gate.creole.SerialAnalyserController").asInstanceOf[SerialAnalyserController]
    pl.add(Factory.createResource("gate.creole.tokeniser.DefaultTokeniser", Factory.newFeatureMap).asInstanceOf[ProcessingResource])
    pl.add(Factory.createResource("gate.creole.splitter.RegexSentenceSplitter", Factory.newFeatureMap()).asInstanceOf[ProcessingResource])
    pl
  }

  override def execute() = {
    import language.postfixOps
    import scala.collection.JavaConversions._

    ARQ.init()
    logger.info("== Gate init ==")
    Gate.init()

    logger.info("== Corpus size counting ==")
    val start = getStartPosInArticleNameList.toInt match {
      case x if x <= 0 => 0
      case x => getStartPosInArticleNameList.toInt
    }
    val end = getEndPosInArticleNameList.toInt match {
      case x if x <= 0 => BasicFunction.tryClose(Source.fromFile(Conf.datasetShort_abstractsPath))(_.getLines().size)
      case x => getEndPosInArticleNameList.toInt
    }
    val step = 500
    logger.info(s"Start of extraction from $start to $end")
    logger.info("Total steps: " + (end - start))

    val memCache = new DBpediaLinker(Conf.wikiApi, Conf.lang, Conf.memcachedAddress, Conf.memcachedPort.toInt)
    val hypernymExtractor = new HypernymExtractor(memCache, Conf.lang, Conf.gateJapeGrammar, Conf.outputDir + "/hypoutput" + (if (getEndPosInArticleNameList.toInt > 0) s".$start-$end" else "") + ".log", "resources/TreeTagger/tree-tagger-german-gate", "resources/TreeTagger/tree-tagger-dutch-gate")
    hypernymExtractor.setLoader(new cz.vse.lhd.hypernymextractor.ProcessStatus(end - start))

    try {
      for (offset <- start to end by step) {
        val endBlock = if (offset + step > end) end else offset + step
        val wikicorpus = Factory.newCorpus("WikipediaCorpus")
        NTReader.fromFile(new File(Conf.datasetShort_abstractsPath)) {
          ntIt =>
            ntIt.slice(offset, endBlock).foldLeft(offset) {
              (idx, stmt) =>
                addDocToCorpus(wikicorpus, stmt, idx)
                idx + 1
            }
        }
        if (!wikicorpus.isEmpty) {
          pipeline.setCorpus(wikicorpus)
          pipeline.execute()
          for {
            doc <- wikicorpus
            sa <- doc.getAnnotations.get("Sentence").get("Sentence")
            isaStart = sa.getStartNode
            isaEnd = sa.getEndNode
            if isaStart.getOffset == 0 || isaStart.getOffset == 2
          } {
            try {
              doc.setContent(doc.getContent.getContent(isaStart.getOffset, isaEnd.getOffset))
            } catch {
              case exc: gate.util.InvalidOffsetException => logger.info(exc.getMessage)
            }
          }
          hypernymExtractor.extractHypernyms(wikicorpus)
        }
      }
    } finally {
      memCache.close()
      hypernymExtractor.close()
    }

    logger.info("== Done ==")
  }

  private def addDocToCorpus(corpus: Corpus, shortAbstractStmt: Statement, idx: Int): Unit = {
    val doc = Factory.newDocument(shortAbstractStmt.getObject.asLiteral().getString)
    val resourceUri = shortAbstractStmt.getSubject.getURI
    doc.setName("doc-" + idx)
    doc.getFeatures.put("article_title", resourceUri.replaceFirst(s"^${Conf.dbpediaBasicUri}resource/", ""))
    doc.getFeatures.put(
      "wikipedia_url",
      resourceUri
        .replace("dbpedia.org/", "wikipedia.org/")
        .replace("/resource/", "/wiki/"))
    doc.getFeatures.put("dbpedia_url", resourceUri)
    doc.getFeatures.put("lang", Conf.lang)
    corpus.add(doc)
  }

}