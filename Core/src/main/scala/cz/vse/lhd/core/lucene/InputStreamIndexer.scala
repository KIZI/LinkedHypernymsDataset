package cz.vse.lhd.core.lucene

import java.io.{Closeable, InputStream}

/**
 * Created by propan on 10. 4. 2015.
 */
trait InputStreamIndexer[T] extends Closeable {

  def index(inputStreams: InputStream*)

  def search[A](ibr : (String => Seq[T]) => A) : A

}
