package cz.vse.lhd.typeinferrer

/**
 * Created by propan on 8. 4. 2015.
 */
trait TypeReader {

  def isInstance(instance: String): Boolean

  def isInstanceType(instance: String, instanceType: String): Boolean

  def getInstanceTypes(instance: String): Seq[String]

}
