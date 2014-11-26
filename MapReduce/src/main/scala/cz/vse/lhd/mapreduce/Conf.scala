package cz.vse.lhd.mapreduce

import cz.vse.lhd.core.AppConf
import cz.vse.lhd.core.ConfGlobal
import cz.vse.lhd.core.AnyToInt

object Conf extends ConfGlobal {

  val (
    intervalLength,
    mavenCommand,
    globalPropertiesFile,
    offset,
    limit
  ) = {
    val (mvnIndex, confIndex, offset, limit) = AppConf.args match {
      case Array(_, AnyToInt(offset), AnyToInt(limit), _ @ _*) => (3, 4, Some(offset), Some(limit))
      case _ => (1, 2, None, None)
    }
    (
      AppConf.args(0).toInt,
      if (AppConf.args.isDefinedAt(mvnIndex))
        AppConf.args(mvnIndex)
      else
        "mvn"
      ,
      if (AppConf.args.isDefinedAt(confIndex))
        AppConf.args(confIndex)
      else
        "../global.properties"
      ,
      offset,
      limit
    )
  }
  
}

object Logger extends cz.vse.lhd.core.Logger {
  val conf = Conf
}