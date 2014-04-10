package cz.vse.lhd.lhdontologycleanup

import cz.vse.lhd.core.AppConf

object LHDOntologyCleanup extends AppConf {

  THDOntologyCleanup.run(AppConf.args)
  
}
