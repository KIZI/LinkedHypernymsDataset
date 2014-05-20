package cz.vse.lhd.hypernymextractor.builder

import com.hp.hpl.jena.query.ARQ
import com.hp.hpl.jena.rdf.model.ModelFactory
import cz.vse.lhd.core.lucene.LuceneReader
import cz.vse.lhd.hypernymextractor.Conf
import cz.vse.lhd.hypernymextractor.Loader
import cz.vse.lhd.hypernymextractor.Logger
import cz.vse.lhd.hypernymextractor.indexbuilder.ArticleDocument
import gate.Corpus
import gate.Factory
import gate.Gate
import gate.ProcessingResource
import gate.creole.SerialAnalyserController
import java.io.ByteArrayInputStream
import org.apache.lucene.index.Term
import scala.io.Source

class CorpusBuilderPR2 extends CorpusBuilderPR {
  
  val pipeline = {
    val pl = Factory.createResource("gate.creole.SerialAnalyserController").asInstanceOf[SerialAnalyserController]
    pl.add(Factory.createResource("gate.creole.tokeniser.DefaultTokeniser", Factory.newFeatureMap).asInstanceOf[ProcessingResource])
    pl.add(Factory.createResource("gate.creole.splitter.RegexSentenceSplitter", Factory.newFeatureMap()).asInstanceOf[ProcessingResource])
    pl
  }
  
  override def execute = {
    import scala.collection.JavaConversions._
    import language.postfixOps
    
    ARQ.init
  
    Logger.get.info("== Gate init ==")
    Gate.init
    
    Logger.get.info("== Corpus size counting ==")
    val start = getStartPosInArticleNameList.toInt match {
      case x if x <= 0 => 0
      case x => getStartPosInArticleNameList.toInt
    }
    val end = getEndPosInArticleNameList.toInt match {
      case x if x <= 0 => Source.fromFile(Conf.datasetLabelsPath).getLines.size
      case x => getEndPosInArticleNameList.toInt
    }
    val step = 500
    Logger.get.info(s"Start of extraction from $start to $end")
    Logger.get.info("Total steps: " + (end - start))
    
    HypernymExtractor.init(Conf.lang, Conf.gateJapeGrammar, Conf.outputDir + "/hypoutput" + (if (getEndPosInArticleNameList.toInt > 0) s".$start-$end" else "") + ".log", "resources/TreeTagger/tree-tagger-german-gate", "resources/TreeTagger/tree-tagger-dutch-gate", getSaveInTriplets)
    if (getSaveInTriplets)
      DBpediaLinker.init(Conf.wikiApi, Conf.lang, Conf.memcachedAddress, Conf.memcachedPort.toInt)
    HypernymExtractor.setLoader(new Loader(end - start))
    
    try {
      for (offset <- start to end by step) {
        val endBlock = if (offset + step > end) end else offset + step
        val lr = LuceneReader.apply(Conf.indexDir)
        val wikicorpus = Factory.newCorpus("WikipediaCorpus")
        try {
          Source
          .fromFile(Conf.datasetLabelsPath)
          .getLines
          .slice(offset, endBlock)
          .map(line => {
              val model = ModelFactory.createDefaultModel
              model.read(new ByteArrayInputStream(line.toString().getBytes()), null, "N-TRIPLE")
              model.listStatements.toList match {
                case x if !x.isEmpty => {
                    val stmt = x.get(0)
                    (Some(stmt), lr.select(new Term(ArticleDocument.strId, stmt.getSubject.getURI), 1))
                  }
                case _ => (None, Nil)
              }
            }
          )
          .foldLeft(offset){
            case (idx, (Some(stmt), ArticleDocument(ad) :: _)) => {
                addDocToCorpus(wikicorpus, stmt.getObject.asLiteral.getString, ad, idx)
                idx + 1
              }
            case (idx, _) => {
                HypernymExtractor.getLoader.tryPrint
                HypernymExtractor.setLoader(HypernymExtractor.getLoader++)
                idx
              }
          }
        } finally {
          lr.close
        }

        if (!wikicorpus.isEmpty) {
          if (getFirstSentenceOnly) {
            pipeline.setCorpus(wikicorpus)
            pipeline.execute
            for {
              doc <- wikicorpus
              sa <- doc.getAnnotations().get("Sentence").get("Sentence")
              isaStart = sa.getStartNode
              isaEnd = sa.getEndNode
              if isaStart.getOffset == 0 || isaStart.getOffset == 2
            } {
              try {
                doc.setContent(doc.getContent.getContent(isaStart.getOffset, isaEnd.getOffset))
              } catch {
                case exc : gate.util.InvalidOffsetException => Logger.get.info(exc.getMessage)
              }
            }
          }
          HypernymExtractor.getInstance().extractHypernyms(wikicorpus);
        }
      }
    } finally {
      if (getSaveInTriplets)
        DBpediaLinker.close
      HypernymExtractor.close
    }
    
    Logger.get.info("== Done ==")
  }
  
  private def addDocToCorpus(corpus : Corpus, title : String, ad : ArticleDocument, idx : Int) : Unit = {
    val doc = Factory.newDocument(ad.sabs)
    doc.setName("doc-" + idx)
    doc.getFeatures.put("article_title", title)
    doc.getFeatures.put(
      "wikipedia_url",
      ad
      .url
      .replaceAllLiterally("dbpedia.org/", "wikipedia.org/")
      .replaceAllLiterally("/resource/", "/wiki/")
    )
    doc.getFeatures.put("dbpedia_url", ad.url)
    if (getAssignDBpediaTypes)
      ad.etype.foldLeft(0)((r, t) => {
          doc.getFeatures.put("db_type_" + r, t)
          r + 1
        }
      )
    doc.getFeatures().put("lang", Conf.lang)
    corpus.add(doc)
  }
  
}