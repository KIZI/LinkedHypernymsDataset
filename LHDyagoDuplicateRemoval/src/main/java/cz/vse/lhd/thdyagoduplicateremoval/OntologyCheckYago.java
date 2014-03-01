package cz.vse.lhd.thdyagoduplicateremoval;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author tomas
 */

public class OntologyCheckYago {

    //key = class ,  value list of  superclass
    private  HashMap<String,String[]> allITaxonomy = new HashMap();
    /*
     * example input line
     * <http://dbpedia.org/resource/Autism> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Disease> .
     */
    public OntologyCheckYago(String path)throws IOException
    {        
        readAllITaxonomy(path);

    }
  
    /*public boolean isType(String name)
    {
        if (allITaxonomy.containsKey(name))
        {
            return true;
        }
        else if (allITaxonomy.containsValue(name))
        {
            return true;
        }
        else
        {
            return false;
        }
        //return allInstances.contains(name);
    }*/
    public String[] getDirectSupertypes(String type)
    {
        return allITaxonomy.get(type);
    }
    /*public boolean isInstanceType(String instanceName, String instanceType)
    {
        String[] instanceTypes = allITaxonomy.get(instanceName);
        if (instanceTypes==null)
        {
            return false;
        }
        for (String type: instanceTypes)
        {
            if (type.equals(instanceType))
            {
                return true;
            }
        }
        return false;                
        //return allInstances.contains(name);
    }*/
   
    private void readAllITaxonomy(String path) throws IOException
    {
//                    Pattern classPattern  = Pattern.compile("<owl:Class rdf:about=\"([^\"]+)\">(.*?)</owl:Class>", Pattern.DOTALL);    
             
             FileInputStream fstream = new FileInputStream(path);
             // Get the object of DataInputStream
             DataInputStream in = new DataInputStream(fstream);
             BufferedReader br = new BufferedReader(new InputStreamReader(in));                    
             StringBuilder all = new StringBuilder();
             String thisLine;
             int lineCounter  = 0;
             try {
                 
             
             while ((thisLine = br.readLine()) != null)
             {
                 lineCounter++;
                 if (thisLine.startsWith("#") | !thisLine.contains("rdfs:subClassOf")) 
                 {
                     continue;
                 }
                         if (lineCounter > THDyagoOverlapDetection.maxLines)
                 {
                     System.out.println("Reached maxLines, quitting InstanceCheck.readAllInstances" );
                     break;
                 }
                         
                 int indexOfSubjectEnd=thisLine.indexOf(">");
                 if (indexOfSubjectEnd<0)
                 {
                     //this happens e.g. for line ""xsd:gYear	rdfs:subClassOf	xsd:integer ."
                     continue;
                 }
                 String subject = thisLine.substring(1, indexOfSubjectEnd);
                 
                 String subjectName = subject;
                 
                 //yago files are not url encoded
               /*  try {
                 subjectName = URLDecoder.decode(subjectName, "UTF-8");
                 }
                 catch (java.lang.IllegalArgumentException e)
                 {
                     //this happens e.g. for "<%>	rdfs:subClassOf	xsd:decimal ."
                     //System.err.println(e.toString());
                 }*/
                 
                 int indexOfObjectStart = thisLine.lastIndexOf("<");//indexOfObjectEnd + predicateLength + 3;
                 int indexOfObjectEnd = thisLine.lastIndexOf(">");
                 //!!!! the following line was corrected: at the end -1 was removed (objectName was truncated)
                 String objectName = thisLine.substring(indexOfObjectStart+1,indexOfObjectEnd);
                // objectName=URLDecoder.decode(objectName, "UTF-8");
                 String[] types=allITaxonomy.get(subjectName);
                 if (types == null)
                 {
                     types  = new String[1];
                     types[0] = objectName;
                 }
                 else
                 {
                     String newTypes[] = new String[types.length+1];
                     for(int i = 0 ; i < types.length ; i++){
                        newTypes[i] = types[i];
                        }
                     newTypes[newTypes.length -1] = objectName;
                     types = newTypes;
                 }
                 allITaxonomy.put(subjectName,types);
             }
             }
             catch (java.lang.OutOfMemoryError e)
             {
                 System.err.println(lineCounter);
                 
             }
    }
    
}
