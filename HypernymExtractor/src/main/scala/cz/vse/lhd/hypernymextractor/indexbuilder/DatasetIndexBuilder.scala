package cz.vse.lhd.hypernymextractor.indexbuilder

import com.hp.hpl.jena.query.ARQ
import com.hp.hpl.jena.rdf.model.ModelFactory
import com.hp.hpl.jena.rdf.model.Statement
import cz.vse.lhd.core.TraversableUtils
import cz.vse.lhd.core.lucene.LuceneReader
import cz.vse.lhd.core.lucene.LuceneWriter
import cz.vse.lhd.hypernymextractor.Conf
import java.io.ByteArrayInputStream
import scala.io.Source
import org.apache.lucene.index.Term

object DatasetIndexBuilder {
  
  def main(args : Array[String]) : Unit = {
    ARQ.init
    indexAbstracts(buildDatasetIterator(Conf.datasetShort_abstractsPath))
    indexTypes(buildDatasetIterator(Conf.datasetInstance_typesPath))
  }
  
  private def buildDatasetIterator(datasetPath : String) = {
    import scala.collection.JavaConversions._
    Source.fromFile(datasetPath).getLines map (line => {
        val model = ModelFactory.createDefaultModel
        model.read(new ByteArrayInputStream(line.toString().getBytes()), null, "N-TRIPLE")
        model.listStatements.toList.toList
      }
    )
  }
  
  private def indexTypes(it : Iterator[List[Statement]]) = {
    var i = 0
    val lr = LuceneReader(Conf.indexDir)
    try {
      LuceneWriter.update(Conf.indexDir)(
        TraversableUtils.lazySortedSeqGroupBy(
          it collect {
            case x :: _ => x
          }
        )(x => x.getSubject.getURI) map (x => x -> lr.select(new Term(ArticleDocument.strId, x.head.getSubject.getURI), 1)) collect {
          case (x, ArticleDocument(ad) :: _) => {
              i = i + 1
              if (i % 50000 == 0)
                println("types added: " + i)
              (new Term(ArticleDocument.strId, ad.url), ad.etype(x map (_.getObject.asResource.getURI)))
            }
        }
      )
    } finally {
      lr.close
    }
    println("total types added: " + i)
  }
  
  private def indexAbstracts(it : Iterator[List[Statement]]) = {
    var i = 0
    LuceneWriter.insert(Conf.indexDir)(
      it collect {
        case x :: _ => {
            i = i + 1
            if (i % 100000 == 0)
              println("indexed pages: " + i)
            new ArticleDocument(
              x.getSubject.getURI,
              x
              .getObject
              .asLiteral
              .getString
              .replaceAll("""(\([^\)]+\))|(\[[^\]]+\])""", "")
            )
          }
      }
    )
    println("total indexed pages: " + i)
  }
  
}
