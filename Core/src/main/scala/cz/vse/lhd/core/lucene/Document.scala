package cz.vse.lhd.core.lucene

import org.apache.lucene.document.Document

trait LuceneDocument {

  def toDocument : Document
  
}
