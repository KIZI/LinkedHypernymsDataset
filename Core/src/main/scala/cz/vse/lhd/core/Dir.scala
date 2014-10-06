package cz.vse.lhd.core

object Dir {

  def /:(path : String) = {
    if (path.endsWith("/"))
      path
    else
      path + "/"
  }
  
}
