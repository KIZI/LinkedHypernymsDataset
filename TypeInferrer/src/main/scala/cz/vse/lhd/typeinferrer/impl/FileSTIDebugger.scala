package cz.vse.lhd.typeinferrer.impl

import java.io.{File, PrintWriter}

import cz.vse.lhd.core.BasicFunction._
import cz.vse.lhd.typeinferrer.{HypernymDebugger, STIDebugger}

/**
 * Created by propan on 26. 5. 2015.
 */
class FileSTIDebugger private(printWriter: PrintWriter) extends STIDebugger {

  def debug(hypernymDebugger: HypernymDebugger): Unit = {
    printWriter.println(hypernymDebugger.build)
    printWriter.println("")
  }

}

object FileSTIDebugger {

  def apply(output: File)(debug: FileSTIDebugger => Unit) = tryClose(new PrintWriter(output, "UTF-8")) { pw =>
    val fsd = new FileSTIDebugger(pw)
    debug(fsd)
  }

}

class SimpleHypernymDebugger(val hypernym: String)(implicit result: String = "") extends HypernymDebugger {

  private def makeStrCandidates(typeFrequency: Map[String, Int], totalFrequency: Int, header: String) = {
    val totalFreqFloat = totalFrequency.toFloat
    typeFrequency.toList.sortBy(_._2).foldLeft(header + "\n") {
      case (x, (strType, freq)) => x + s"$strType, $freq, ${freq / totalFreqFloat}\n"
    }
  }

  def selectedType(selType: String, confidence: Float): HypernymDebugger = {
    new SimpleHypernymDebugger(hypernym)(result + s"# selected mapping, confidence\n$selType, $confidence")
  }

  def allCandidates(typeFrequency: Map[String, Int], totalFrequency: Int): HypernymDebugger = {
    new SimpleHypernymDebugger(hypernym)(result + makeStrCandidates(typeFrequency, totalFrequency, "# list of candidate mapped types, frequency, confidence"))
  }

  def prunedCandidates(typeFrequency: Map[String, Int], totalFrequency: Int): HypernymDebugger = {
    new SimpleHypernymDebugger(hypernym)(result + makeStrCandidates(typeFrequency, totalFrequency, "# pruned set of types, frequency, confidence"))
  }

  def build: String = s"# type\n$hypernym\n$result"
}
