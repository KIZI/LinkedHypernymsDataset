package cz.vse.lhd.typeinferrer

/**
 * Created by propan on 26. 5. 2015.
 */
trait STIDebugger {

  def debug(hypernymDebugger: HypernymDebugger)

}

trait HypernymDebugger {

  val hypernym: String

  def allCandidates(typeFrequency: Map[String, Int], totalFrequency: Int): HypernymDebugger

  def prunedCandidates(typeFrequency: Map[String, Int], totalFrequency: Int): HypernymDebugger

  def selectedType(selType: String, confidence: Float): HypernymDebugger

  def build: String

}
