package cz.vse.lhd.core.lucene

import org.apache.commons.io.FileUtils
import org.apache.lucene.analysis.core.SimpleAnalyzer
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
import org.apache.lucene.util.Version
import org.apache.lucene.index.IndexWriterConfig.OpenMode

class LuceneWriter private (mode : OpenMode, strDir : String) extends Lucene(strDir) {

  private val writer = {
    if (mode == OpenMode.CREATE)
      FileUtils.cleanDirectory(dir.getDirectory)
    new IndexWriter(dir, new IndexWriterConfig(Version.LUCENE_46, new SimpleAnalyzer(Version.LUCENE_46)))
  }
  
  def insert(doc : LuceneDocument) = writer.addDocument(doc.toDocument);
  
  def update(term : Term, doc : LuceneDocument) = writer.updateDocument(term, doc.toDocument)
  
  def delete(term : Term) = writer.deleteDocuments(term)
  
  def close = {
    writer.forceMerge(1)
    writer.close
  }
  
}

object LuceneWriter {
  
  def insert(dir : String)(trv : TraversableOnce[LuceneDocument]) = {
    val lw = new LuceneWriter(OpenMode.CREATE, dir)
    try {
      trv foreach lw.insert
    } finally {
      lw.close
    }
  }
  
  def update(dir : String)(trv : TraversableOnce[(Term, LuceneDocument)]) = {
    val lw = new LuceneWriter(OpenMode.APPEND, dir)
    try {
      for ((t, d) <- trv)
        lw update (t, d)
    } finally {
      lw.close
    }
  }
  
  def delete(dir : String)(trv : TraversableOnce[Term]) = {
    val lw = new LuceneWriter(OpenMode.CREATE_OR_APPEND, dir)
    try {
      trv foreach lw.delete
    } finally {
      lw.close
    }
  }
  
}