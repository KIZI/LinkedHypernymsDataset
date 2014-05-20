package cz.vse.lhd.mapreduce

trait MapReduce {

  def map : MapReduce
  def reduce : MapReduce
  
}