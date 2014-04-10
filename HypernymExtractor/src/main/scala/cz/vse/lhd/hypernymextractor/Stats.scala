package cz.vse.lhd.hypernymextractor

import cz.vse.lhd.core.AppConf
import scala.io.Source

object Stats extends AppConf {
  
  println(Source.fromFile(Conf.datasetLabelsPath).getLines.size)
  
}