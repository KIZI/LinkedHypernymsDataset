package cz.vse.lhd.core.lucene

import java.io.File
import org.apache.lucene.store.FSDirectory

class LuceneException(msg : String) extends Exception("LUCENE: " + msg)

abstract class Lucene(strDir : String) {
  
  val dir = {
    val dirf = new File(strDir)
    if (!dirf.isDirectory)
      dirf.mkdirs
    if (!dirf.isDirectory || !dirf.canRead || !dirf.canWrite)
      throw new LuceneException("Target dir does not exist or is not readable or is not writable.")
    FSDirectory.open(dirf)
  }
  
}