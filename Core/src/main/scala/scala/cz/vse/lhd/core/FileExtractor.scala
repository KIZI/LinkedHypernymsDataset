package cz.vse.lhd.core

import java.io.File

object FileExtractor {

  def unapply(path : String) = {
    val f = new File(path)
    if (f.isFile && f.canRead && f.canWrite)
      Some(f)
    else
      None
  }
  
}
