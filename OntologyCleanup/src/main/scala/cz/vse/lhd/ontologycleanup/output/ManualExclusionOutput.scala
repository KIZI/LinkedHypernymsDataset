package cz.vse.lhd.ontologycleanup.output

import java.io.{File, OutputStream}

import cz.vse.lhd.core.{NTReader, NTWriter}
import cz.vse.lhd.ontologycleanup.ManualMapping

/**
 * Created by propan on 16. 5. 2015.
 */
class ManualExclusionOutput(input: File, manualMapping: ManualMapping) extends OutputMaker with OutputMakerHeader with OutputMakerFooter {

  private var excludedCount = 0
  private var notExcludedCount = 0

  val header = "# Instances with type on blacklist removed"
  lazy val footer = "# DBpedia instances excluded = " + excludedCount + ", not excluded = " + notExcludedCount

  def makeFile(output: OutputStream) = NTReader.fromFile(input) { it =>
    NTWriter.fromIterator(
      it.filter { stmt =>
        if (manualMapping.isExcluded(stmt.getObject.asResource().getURI)) {
          excludedCount = excludedCount + 1
          false
        } else {
          notExcludedCount = notExcludedCount + 1
          true
        }
      },
      output
    )
  }

}