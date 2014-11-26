package cz.vse.lhd.mapreduce

import cz.vse.lhd.core.AppConf
import cz.vse.lhd.mapreduce.cmd.HypenymExtractionCommand
import cz.vse.lhd.mapreduce.cmd.HypenymExtractionCountCommand
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import scala.language.existentials

object LocalMapReduce extends AppConf {
  
  val mapReduce = new MapReduce {
    
    def map = {
      new HypenymExtractionCountCommand(count => {
          val start = Conf.offset.getOrElse(0)
          val end = Conf.limit.map(_ + start).getOrElse(count)
          start.to(end, Conf.intervalLength).par foreach (start => {
              val end = start + Conf.intervalLength
              val hec = new HypenymExtractionCommand(start, end)
              hec.execute
            }
          )
        }).execute
      this
    }
    
    def reduce = {
      for ((ft, files) <- new File(Conf.outputDir).listFiles filter (_.getName.matches("""hypoutput\.\d+-\d+.*""")) groupBy (_.getName.replaceAll(""".+\.""", ""))) {
        val result = new BufferedOutputStream(new FileOutputStream(Conf.outputDir + "hypoutput.log." + ft))
        try {
          for (file <- files) {
            val source = new BufferedInputStream(new FileInputStream(file))
            try {
              Stream continually (source.read) takeWhile (_ != -1) foreach result.write
            } finally {
              source.close
            }
            file.delete
          }
        } finally {
          result.close
        }
      }
      this
    }
    
  }
  
  mapReduce.map.reduce
  
}
