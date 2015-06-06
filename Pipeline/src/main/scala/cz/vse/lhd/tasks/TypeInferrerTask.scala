package cz.vse.lhd.tasks

import cz.vse.lhd.Task
import cz.vse.lhd.typeinferrer.{Conf, StatisticalTypeInferrer}

/**
 * Created by propan on 6. 6. 2015.
 */
class TypeInferrerTask extends Task {

  def run(): Unit = {
    StatisticalTypeInferrer.main(Array(Conf.globalPropertiesFile))
  }

}
