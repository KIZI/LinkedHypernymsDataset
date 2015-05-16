package cz.vse.lhd.core.lucene

import java.io.File

import cz.vse.lhd.core.BasicFunction._
import cz.vse.lhd.core.RdfTriple
import org.apache.lucene.analysis.core.KeywordAnalyzer
import org.apache.lucene.document.{Document, Field, StoredField, StringField}
import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig, Term}
import org.apache.lucene.search.{IndexSearcher, TermQuery}
import org.apache.lucene.store.{Directory, FSDirectory}
import org.slf4j.LoggerFactory

/**
 * Created by propan on 10. 4. 2015.
 */
class NTIndexer(indexDir: File) extends InputStreamIndexer[RdfTriple] {

  def this(indexDirStr: String) = this(new File(indexDirStr))

  lazy val logger = LoggerFactory.getLogger(classOf[NTIndexer])

  val directory: Directory = {
    if (!indexDir.isDirectory) indexDir.mkdirs()
    FSDirectory.open(indexDir.toPath)
  }

  private def logProgress(counter: Int, forceLog: Boolean): Unit = {
    if (counter % 100000 == 0 || forceLog)
      logger.info("Indexed triples: " + counter)
  }

  private def searchByKey(key: String)(implicit is: IndexSearcher): Seq[RdfTriple] = is
    .search(new TermQuery(new Term("subject", key)), 1000)
    .scoreDocs
    .map {
    hit =>
      val hitDoc = is.doc(hit.doc)
      RdfTriple(key, hitDoc.get("predicate"), hitDoc.get("object"))
  }

  def index(inputIterator: Iterator[RdfTriple]): Unit = tryClose(new IndexWriter(directory, new IndexWriterConfig(new KeywordAnalyzer))) {
    implicit iw =>
      val counter = inputIterator.foldLeft(0) {
        (counter, stmt) =>
          val doc = new Document
          doc.add(new StringField("subject", stmt.subject, Field.Store.NO))
          doc.add(new StoredField("predicate", stmt.predicate))
          doc.add(new StoredField("object", stmt.`object`))
          iw.addDocument(doc)
          logProgress(counter + 1, false)
          counter + 1
      }
      logProgress(counter, true)
  }

  def search[A](ibr: ((String) => Seq[RdfTriple]) => A): A = tryClose(DirectoryReader.open(directory)) {
    dr =>
      implicit val is = new IndexSearcher(dr)
      ibr(searchByKey)
  }

  def close(): Unit = directory.close()

}