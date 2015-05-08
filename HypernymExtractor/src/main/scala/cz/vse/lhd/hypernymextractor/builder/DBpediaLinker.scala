package cz.vse.lhd.hypernymextractor.builder

import java.net.{URL, URLEncoder}

import cz.vse.lhd.core.BasicFunction._
import cz.vse.lhd.hypernymextractor.Conf
import org.slf4j.LoggerFactory

import scala.io.Source

/**
 * Created by propan on 17. 4. 2015.
 */
class DBpediaLinker(apiBase: String, lang: String) {

  self: ResourceCache =>

  private val logger = LoggerFactory.getLogger(getClass)
  private val ArticlePattern = if (apiBase.contains("search")) "(?m)^\\d.*\\s(\\S+)$".r else "(?m) title=\"([^\"]+)\" ".r.unanchored

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
