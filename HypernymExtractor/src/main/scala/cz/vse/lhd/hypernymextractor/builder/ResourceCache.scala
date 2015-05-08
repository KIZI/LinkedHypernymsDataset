package cz.vse.lhd.hypernymextractor.builder

import java.io.Closeable

/**
 * Created by propan on 8. 5. 2015.
 */
trait ResourceCache extends Closeable {

  def getFromCache(key: String) : Option[String]

  def saveToCache(key: String, value: String)

  def flush()

}
