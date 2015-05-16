package cz.vse.lhd.core

import java.io.OutputStream

import com.hp.hpl.jena.rdf.model.{ModelFactory, Statement}
import cz.vse.lhd.core.BasicFunction._
import org.apache.jena.riot.{RDFDataMgr, RDFFormat}

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

/**
 * Created by propan on 15. 5. 2015.
 */
object NTWriter {

  def write(output: OutputStream)(writer: (Statement => Unit) => Unit): Unit = tryClose(output) { output =>
    val bufferedOutput = ListBuffer.empty[Statement]
    def tryFlush(force: Boolean) = if (force || bufferedOutput.length >= 500) {
      val model = ModelFactory.createDefaultModel
      model.add(bufferedOutput.asJava)
      RDFDataMgr.write(output, model, RDFFormat.NTRIPLES_ASCII)
      bufferedOutput.clear()
    }
    writer { stmt =>
      bufferedOutput += stmt
      tryFlush(false)
    }
    tryFlush(true)
  }

  def fromIterator(it: Iterator[Statement], output: OutputStream) : Unit = write(output) { writer =>
    it foreach writer
  }

  def fromIterator(it: Iterator[RdfTriple], output: OutputStream)(implicit objectType: RdfTriple.ObjectType = RdfTriple.Resource) : Unit = fromIterator(it.map(_.toStatement), output)

}
