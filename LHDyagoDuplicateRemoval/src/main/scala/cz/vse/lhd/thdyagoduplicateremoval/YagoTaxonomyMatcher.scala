package cz.vse.lhd.thdyagoduplicateremoval

import java.util.regex.Pattern

class YagoTaxonomyMatcher(icy : InstanceCheckYago, ocy : OntologyCheckYago) {

  private def buildMatchingRegex(instanceName : String, matchType : MatchType) = {
    var pattern = ""
    if (instanceName.endsWith("s")) {
      pattern = Pattern.quote(instanceName) + "?"
    } else {
      pattern = Pattern.quote(instanceName) + "s?"
    }
    //yago type names have the following format: 
    //wikicategory_Vowel_letters
    //wordnet_saint_110546850
    if (matchType == MatchType.exact) {
      pattern = "wikicategory_" + pattern + "$|" + "wordnet_" + pattern + "_\\d+";
    }
    val p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
    p
  }

  private def checkOverlap(yagoType : String, pattern : Pattern) : String = {
    if (pattern.matcher(yagoType).find()) {
      yagoType
    } else {
      val supertypes = ocy.getDirectSupertypes(yagoType)
      supertypes foreach println
      if (supertypes == null || supertypes.isEmpty) {
        null
      } else {
        supertypes find (_ != yagoType) match {
          case Some(x) => checkOverlap(x, pattern)
          case None => null
        }
      }
    }
  }

  def isInstance(name : String) : java.lang.Boolean = {
    icy.isInstance(name)
    //return allInstances.contains(name);
  }

  /*
   * returns null string if matching YAGO type was not found or no type is assigned
   * otherwise returns a string with the matching type
   */
  def isInstanceType(instanceName : String, instanceType : String, matchType : MatchType) = {
    val yagoInstanceTypes = icy.getTypes(instanceName)
    if (yagoInstanceTypes == null) {
      System.err.println("This should have been detected earlier");
      null
    } else {
      yagoInstanceTypes.foldLeft("")((r, yit) =>
        if (r.isEmpty) {
          val pattern = buildMatchingRegex(instanceType, matchType)
          val overlappingType = checkOverlap(yit, pattern)
          if (overlappingType != null) {
            overlappingType
          } else {
            r
          }
        } else {
          r
        }
      )  
    }
    //return allInstances.contains(name);
  }
  
}
