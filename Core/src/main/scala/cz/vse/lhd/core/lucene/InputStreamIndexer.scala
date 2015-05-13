package cz.vse.lhd.core.lucene

import java.io.Closeable

/**
 * Created by propan on 10. 4. 2015.
 */
trait InputStreamIndexer[T] extends Closeable {

  def index(inputIterator: Iterator[T])

  def search[A](ibr : (String => Seq[T]) => A) : A

}
