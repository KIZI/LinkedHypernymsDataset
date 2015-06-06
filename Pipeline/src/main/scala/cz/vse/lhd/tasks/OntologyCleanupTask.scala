package cz.vse.lhd.tasks

import cz.vse.lhd.Task
import cz.vse.lhd.ontologycleanup.{Conf, LHDOntologyCleanup}

/**
 * Created by propan on 6. 6. 2015.
 */
class OntologyCleanupTask extends Task {

  def run(): Unit = {
    LHDOntologyCleanup.main(Array(Conf.globalPropertiesFile))
  }

}
