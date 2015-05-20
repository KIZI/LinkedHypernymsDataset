package cz.vse.lhd.lhdontologycleanup

import cz.vse.lhd.core.BasicFunction._
import cz.vse.lhd.core.RdfTriple
import cz.vse.lhd.core.lucene.NTIndexer

/**
 * Created by propan on 18. 5. 2015.
 */
class LanguageMapping(searcher: String => Seq[RdfTriple]) {

  def englishResource(resource: String) = {
    searcher(resource).find(_.predicate == "http://www.w3.org/2002/07/owl#sameAs").map(_.`object`)
  }

}

object LanguageMapping {

  def use(f: LanguageMapping => Unit) = tryClose(new NTIndexer(Conf.indexDir)) { indexer =>
    indexer.search { searcher =>
      f(new LanguageMapping(searcher))
    }
  }

  def langByResource(resource: String) = {
    val LangPattern = """http://(.+)\.dbpedia\.org.*""".r
    resource match {
      case LangPattern(lang) => lang
      case _ => "en"
    }
  }

}
