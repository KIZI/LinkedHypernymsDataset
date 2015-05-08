package cz.vse.lhd.hypernymextractor.builder

import org.slf4j.LoggerFactory

import scala.collection.concurrent.TrieMap

/**
 * Created by propan on 25. 4. 2015.
 */
trait LocalResourceCache extends ResourceCache  {

  private val cache = TrieMap.empty[String, String]
  private val logger = LoggerFactory.getLogger(getClass)

  def close(): Unit = flush()

  def getFromCache(key: String) = {
    val cachedValue = cache.get(key)
    cachedValue foreach (x => logger.debug(s"Fetched resource from cache: $key -> $x"))
    cachedValue
  }

  def saveToCache(key: String, value: String) = cache.putIfAbsent(key, value)

  def flush() = cache.clear()

}
