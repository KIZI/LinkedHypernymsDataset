package cz.vse.lhd.core

import java.io.PrintWriter
import java.io.StringWriter
import java.util.Date
import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.LogRecord
import scala.util.Try

trait Logger {

  val conf: ConfGlobal

  lazy val get = {
    val logger = java.util.logging.Logger.getGlobal
    logger.setLevel(Level.INFO)
    logger.setUseParentHandlers(false)
    val handler = new ConsoleHandler
    handler.setFormatter(new java.util.logging.Formatter {
      val LINE_SEPARATOR = System.getProperty("line.separator")
      def format(record: LogRecord) = {
        val sb = new StringBuilder
        sb.append(new Date(record.getMillis()))
          .append(" ")
          .append(record.getLevel().getLocalizedName())
          .append(": ")
          .append(formatMessage(record))
          .append(LINE_SEPARATOR)
        if (record.getThrown() != null) {
          Try {
            val sw = new StringWriter
            val pw = new PrintWriter(sw)
            record.getThrown().printStackTrace(pw)
            pw.close
            sb.append(sw.toString())
          }
        }
        sb.toString
      }
    })
    logger.addHandler(handler)
    logger
  }

}