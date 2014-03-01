package cz.vse.lhd.hypernymextractor.indexbuilder

import cz.vse.lhd.core.lucene.LuceneDocument
import org.apache.lucene.document.Document
import org.apache.lucene.document.StoredField
import org.apache.lucene.document.StringField
import org.apache.lucene.document.Field.Store

class ArticleDocument(val url : String, val sabs : String, val etype : Seq[String]) extends LuceneDocument {

  def this(url : String, sabs : String) = this(url, sabs, Nil)
  
  def toDocument = {
    val doc = new Document
    doc.add(new StringField(ArticleDocument.strId, url, Store.YES))
    doc.add(new StoredField(ArticleDocument.strAbstract, sabs))
    if (!etype.isEmpty)
      doc.add(new StoredField(ArticleDocument.strType, etype.mkString(";")))
    doc
  }
  
  def etype(etype : Seq[String]) = new ArticleDocument(url, sabs, etype)
  
}

object ArticleDocument {
  
  val strId = "id"
  val strAbstract = "sabs"
  val strType = "etype"
  
  def unapply(doc : Document) : Option[ArticleDocument] = Some(new ArticleDocument(
      doc.get(strId),
      doc.get(strAbstract),
      doc.get(strType) match {
        case null => Nil
        case x => x.split(';')
      }
    )
  )
  
}
