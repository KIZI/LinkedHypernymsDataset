package cz.vse.lhd.hypernymextractor.builder

import java.io.Closeable
import java.net.InetSocketAddress
import java.security.MessageDigest
import java.util

import net.spy.memcached.{FailureMode, ConnectionFactoryBuilder, MemcachedClient}
import org.slf4j.LoggerFactory

/**
 * Created by propan on 25. 4. 2015.
 */
trait MemCached extends Closeable {

  val address: String
  val port: Int

  private val logger = LoggerFactory.getLogger(getClass)
  private val memClient = new MemcachedClient(new ConnectionFactoryBuilder().setDaemon(true).setFailureMode(FailureMode.Retry).build(), util.Arrays.asList(new InetSocketAddress(address, port)))

  def close(): Unit = {
    memClient.shutdown()
  }

  private def md5(str: String) = MessageDigest.getInstance("MD5").digest(str.getBytes).foldLeft("") {
    (result, byte) =>
      result + Integer.toHexString((byte & 0xFF) | 0x100).substring(1, 3)
  }

  private def normKey(key: String) = if (key.length > 200) md5(key) else key

  def getFromCache(key: String) = Option(memClient.get(normKey(key))).map {
    cachedVal =>
      val cachedStrVal = cachedVal.asInstanceOf[String]
      logger.debug(s"Fetched resource from cache: $key -> $cachedStrVal")
      cachedStrVal
  }

  def saveToCache(key: String, value: String) = Option(value).foreach(x => memClient.set(normKey(key), 604800, x))

  def flush = memClient.flush()

}
