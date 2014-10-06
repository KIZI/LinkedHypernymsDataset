package cz.vse.lhd.core.lucene

import org.apache.lucene.analysis.core.SimpleAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery
import org.apache.lucene.util.Version

class LuceneReader private (strDir : String) extends Lucene(strDir) {
  
  private val reader = new IndexSearcher(DirectoryReader.open(dir))
  private val qp = new QueryParser(Version.LUCENE_46, "", new SimpleAnalyzer(Version.LUCENE_46))
  
  def select(query : Query, limit : Int) : List[Document] = reader
  .search(query, limit)
  .scoreDocs
  .map(x => reader.doc(x.doc))
  .toList
  
  def select(query : String, limit : Int) : List[Document] =
    select(qp.parse(query), limit)
  
  def select(query : Term, limit : Int) : List[Document] =
    select(new TermQuery(query), limit)

  def close = reader.getIndexReader.close
  
}

object LuceneReader {
  
  def apply(dir : String) = new LuceneReader(dir)
  
}