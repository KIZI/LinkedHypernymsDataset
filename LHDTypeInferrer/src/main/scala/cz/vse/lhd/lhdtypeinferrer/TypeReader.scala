package cz.vse.lhd.lhdtypeinferrer

/**
 * Created by propan on 8. 4. 2015.
 */
trait TypeReader {

  def isInstance: Boolean

  def isInstanceType(instanceName: String, instanceType: String): Boolean

  def getInstanceTypes(instanceName: String): Seq[String]

}
