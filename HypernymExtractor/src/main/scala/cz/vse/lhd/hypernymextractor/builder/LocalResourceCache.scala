package cz.vse.lhd.hypernymextractor.builder

import scala.collection.concurrent.TrieMap

/**
 * Created by propan on 25. 4. 2015.
 */
trait LocalResourceCache extends ResourceCache {

  private val cache = TrieMap.empty[String, String]

  def close(): Unit = flush()

  def getFromCache(key: String) = cache.get(key)

  def saveToCache(key: String, value: String) = cache.putIfAbsent(key, value)

  def flush() = cache.clear()

}
