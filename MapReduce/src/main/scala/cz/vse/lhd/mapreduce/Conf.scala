package cz.vse.lhd.mapreduce

import cz.vse.lhd.core.AppConf
import cz.vse.lhd.core.ConfGlobal
import cz.vse.lhd.core.AnyToInt

object Conf extends ConfGlobal {

  val (
    globalPropertiesFile,
    intervalLength,
    mavenCommand,
    offset,
    limit
  ) = {
    val (mvnIndex, offset, limit) = AppConf.args match {
      case Array(_, _, AnyToInt(offset), AnyToInt(limit), _ @ _*) => (4, Some(offset), Some(limit))
      case _ => (2, None, None)
    }
    (
      AppConf.args(0).toString,
      AppConf.args(1).toInt,
      if (AppConf.args.isDefinedAt(mvnIndex))
        AppConf.args(mvnIndex)
      else
        "mvn"
      ,
      offset,
      limit
    )
  }
  
}

object Logger extends cz.vse.lhd.core.Logger {
  val conf = Conf
}