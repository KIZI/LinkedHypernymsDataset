package cz.vse.lhd.hypernymextractor

import java.io.File

import cz.vse.lhd.core.BasicFunction._
import cz.vse.lhd.core.NTReader

import scala.io.Source

object Stats {

  def countResources(dataset: File) = tryClose(Source.fromFile(dataset, "UTF-8")) {
    source =>
      NTReader.fromIterator(source.getLines()).size
  }

}