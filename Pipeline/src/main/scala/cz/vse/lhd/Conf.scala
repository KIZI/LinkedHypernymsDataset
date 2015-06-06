package cz.vse.lhd

import cz.vse.lhd.core.{AppConf, ConfGlobal}

object Conf extends ConfGlobal {

  val globalPropertiesFile = AppConf.args(0)

}