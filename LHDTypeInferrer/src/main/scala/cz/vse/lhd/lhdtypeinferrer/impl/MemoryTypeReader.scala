package cz.vse.lhd.lhdtypeinferrer.impl

import java.io.File

import cz.vse.lhd.core.NTReader
import cz.vse.lhd.lhdtypeinferrer.TypeReader

/**
 * Created by propan on 9. 4. 2015.
 */
class MemoryTypeReader(dataset: File) extends TypeReader {

  val types = collection.mutable.Map.empty[String, collection.mutable.ListBuffer[String]]

  NTReader.fromFile(dataset) {
    it =>
      for (stmt <- it)
        types.getOrElseUpdate(stmt.getSubject.getURI, collection.mutable.ListBuffer.empty) += stmt.getObject.asResource().getURI
  }

  def isInstance(instance: String): Boolean = types.contains(instance)

  def isInstanceType(instance: String, instanceType: String): Boolean = types.get(instance).exists(_.contains(instanceType))

  def getInstanceTypes(instance: String): Seq[String] = types.getOrElse(instance, Nil)
}
