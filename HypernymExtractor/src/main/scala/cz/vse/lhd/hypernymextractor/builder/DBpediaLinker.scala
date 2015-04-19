package cz.vse.lhd.hypernymextractor.builder

import java.io.Closeable
import java.net.{InetSocketAddress, URL, URLEncoder}
import java.security.MessageDigest
import java.util

import cz.vse.lhd.core.BasicFunction._
import cz.vse.lhd.hypernymextractor.Conf
import net.spy.memcached.{ConnectionFactoryBuilder, FailureMode, MemcachedClient}
import org.slf4j.LoggerFactory

import scala.io.Source

/**
 * Created by propan on 17. 4. 2015.
 */
class DBpediaLinker(apiBase: String, lang: String, address: String, port: Int) extends Closeable {

  private val logger = LoggerFactory.getLogger(getClass)
  private val memClient = new MemcachedClient(new ConnectionFactoryBuilder().setDaemon(true).setFailureMode(FailureMode.Retry).build(), util.Arrays.asList(new InetSocketAddress(address, port)))
  private val ArticlePattern = if (apiBase.contains("search")) "(?m)^\\d.*\\s(\\S+)$".r else "(?m) title=\"([^\"]+)\" ".r.unanchored

  def close(): Unit = {
    memClient.flush()
    memClient.shutdown()
  }

  private def md5(str: String) = MessageDigest.getInstance("MD5").digest(str.getBytes).foldLeft("") {
    (result, byte) =>
      result + Integer.toHexString((byte & 0xFF) | 0x100).substring(1, 3)
  }

  private def normKey(key: String) = if (key.length > 200) md5(key) else key

  private def getFromCache(key: String) = Option(memClient.get(normKey(key))).map {
    cachedVal =>
      val cachedStrVal = cachedVal.asInstanceOf[String]
      logger.debug(s"Fetched resource from cache: $key -> $cachedStrVal")
      cachedStrVal
  }

  private def saveToCache(key: String, value: String) = Option(value).foreach(x => memClient.set(normKey(key), 604800, x))

  private def getUrlContent(url: URL) = retry(10) {
    tryClose(Source.fromURL(url, "UTF-8"))(_.mkString)
  } {
    (e, n) =>
      logger.warn(s"${e.getMessage}. Number of remaining tries: ${n - 1}")
      Thread.sleep((Math.pow(2, n) * 1000).toLong)
  }

  private def fetchLink(input: String) = {
    val normInput = URLEncoder.encode(input.replaceAll(" ", "_"), "UTF-8")
    val url = if (apiBase.contains("search"))
      new URL(apiBase + normInput + "?limit=" + 1)
    else
      new URL(apiBase + "api.php?action=query&format=xml&list=search&srwhat=nearmatch" + "&srlimit=" + 1 + "&srsearch=" + normInput)
    getUrlContent(url) match {
      case ArticlePattern(title) =>
        logger.debug(s"Fetched resource from wiki API: $input -> $title")
        Some(Conf.dbpediaBasicUri + "resource/" + title.replaceAll(" ", "_"))
      case _ =>
        logger.debug(s"No resource fetched for input: $input")
        None
    }
  }

  def getLink(input: String) = getFromCache(input) orElse {
    val resource = fetchLink(input)
    resource.foreach(x => saveToCache(input, x))
    resource
  }

}
