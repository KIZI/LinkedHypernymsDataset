package cz.vse.lhd.mapreduce

import cz.vse.lhd.core.AppConf
import cz.vse.lhd.core.ConfGlobal

object Conf extends ConfGlobal {

  val (
    intervalLength,
    mavenCommand,
    globalPropertiesFile
  ) = {
    (
      AppConf.args(0).toInt,
      if (AppConf.args.isDefinedAt(1))
        AppConf.args(1)
      else
        "mvn"
      ,
      if (AppConf.args.isDefinedAt(2))
        AppConf.args(2)
      else
        "../global.properties"
    )
  }
  
}

object Logger extends cz.vse.lhd.core.Logger {
  val conf = Conf
}