package cz.vse.lhd.hypernymextractor.builder

import com.hp.hpl.jena.rdf.model.ModelFactory
import cz.vse.lhd.core.lucene.LuceneReader
import cz.vse.lhd.hypernymextractor.Conf
import cz.vse.lhd.hypernymextractor.Logger
import cz.vse.lhd.hypernymextractor.indexbuilder.ArticleDocument
import gate.Corpus
import gate.Factory
import gate.Gate
import gate.ProcessingResource
import gate.creole.SerialAnalyserController
import java.io.ByteArrayInputStream
import java.util.logging.Level
import org.apache.lucene.index.Term
import scala.io.Source

class CorpusBuilderPR2 extends CorpusBuilderPR {
 
  val pipeline = {
    val pl = Factory.createResource("gate.creole.SerialAnalyserController").asInstanceOf[SerialAnalyserController]
    pl.add(Factory.createResource("gate.creole.tokeniser.DefaultTokeniser", Factory.newFeatureMap).asInstanceOf[ProcessingResource])
    pl.add(Factory.createResource("gate.creole.splitter.RegexSentenceSplitter", Factory.newFeatureMap()).asInstanceOf[ProcessingResource])
    pl
  }
  lazy val (apiBase, japePath) = Conf.lang match {
    case "en" => (getWikiAPIBase_EN, getJAPEPATH_EN)
    case "de" => (getWikiAPIBase_DE, getJAPEPATH_DE)
    case "nl" => (getWikiAPIBase_NL, getJAPEPATH_NL)
  } 
  
  override def execute = {
    import scala.collection.JavaConversions._
    
    Logger.get.info("== Gate init ==")
    Gate.init
    HypernymExtractor.init(Conf.lang, japePath, Conf.outputDir + "/hypoutput.log", getTaggerBinary_DE, getTaggerBinary_NL, getSaveInTriplets)
    if (getSaveInTriplets)
      DBpediaLinker.init(apiBase, Conf.lang, Conf.memcachedAddress, Conf.memcachedPort.toInt)
        
    Logger.get.info("== Corpus size counting ==")
    
    val size = Source.fromFile(Conf.datasetLabelsPath).getLines.size
    val step = 500
    
    Logger.get.info("Total steps: " + size)
    
    try {
      for (offset <- 0 to size by step) {
        Logger.get.info("== Corpus loading ==")
        Logger.get.log(Level.INFO, s"Loading from ${offset} until ${offset+step}...")
        val lr = LuceneReader.apply(Conf.indexDir)
        val wikicorpus = Factory.newCorpus("WikipediaCorpus")
        try {
          Source
          .fromFile(Conf.datasetLabelsPath)
          .getLines
          .slice(offset, offset + step)
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
          .foldLeft(getStartPosInArticleNameList){
            case (idx, (Some(stmt), ArticleDocument(ad) :: _)) => {
                addDocToCorpus(wikicorpus, stmt.getObject.asLiteral.getString, ad, idx)
                idx + 1
              }
            case (idx, _) => idx
          }
        } finally {
          lr.close
        }

        if (wikicorpus.isEmpty) {
          Logger.get.warning("Corpus is empty.")
        } else {
          if (getFirstSentenceOnly) {
            Logger.get.info("== Sentence splitting ==")
            pipeline.setCorpus(wikicorpus)
            pipeline.execute
            for {
              doc <- wikicorpus
              sa <- doc.getAnnotations().get("Sentence").get("Sentence")
              isaStart = sa.getStartNode
              isaEnd = sa.getEndNode
              if isaStart.getOffset == 0 || isaStart.getOffset == 2
            } {
              doc.setContent(doc.getContent.getContent(isaStart.getOffset, isaEnd.getOffset))
            }
          }
          Logger.get.info("== Extract hypernym starting ==")
          HypernymExtractor.getInstance().extractHypernyms(wikicorpus);
        }
      }
    } finally {
      if (getSaveInTriplets)
        DBpediaLinker.close
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