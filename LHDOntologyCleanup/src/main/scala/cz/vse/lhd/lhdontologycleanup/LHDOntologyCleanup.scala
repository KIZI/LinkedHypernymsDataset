package cz.vse.lhd.lhdontologycleanup

import java.io.{FileOutputStream, FileInputStream}

import cz.vse.lhd.core.AppConf
import cz.vse.lhd.lhdontologycleanup.output.{OutputHeader, UniqueLinesOutput}

object LHDOntologyCleanup extends AppConf {

  THDOntologyCleanup.run(AppConf.args)

  def makeUniqueHypoutOutput() = {
    OutputHeader.apply(new FileOutputStream(Conf.outputDir + Conf.Output.hypoutDbpediaUnique), "# Input file with duplicate lines removed") { os =>
      UniqueLinesOutput(new FileInputStream(Conf.outputDir + Conf.Output.hypoutDbpedia), os)
    }
  }

  def makeTypeOverrideOutput(manualMapping: ManualMapping) = {

  }

}
