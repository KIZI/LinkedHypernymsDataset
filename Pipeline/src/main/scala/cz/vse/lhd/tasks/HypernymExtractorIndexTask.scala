package cz.vse.lhd.tasks

import cz.vse.lhd.Task
import cz.vse.lhd.hypernymextractor.{Conf, RunDefaultPipeline}

/**
 * Created by propan on 6. 6. 2015.
 */
class HypernymExtractorIndexTask extends Task {

  def run(): Unit = {
    RunDefaultPipeline.main(Array(Conf.globalPropertiesFile, "index"))
  }

}
