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

public class InstanceCheckYago {
    //private static HashSet allInstances = new HashSet();
    private  HashMap<String,String[]> allInstances_types = new HashMap();

    HashSet exceptions = new HashSet();
    //private static int predicateLength = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>".length();
    private boolean onlyDBpediantologyTypes  = false;
    /*
     * example input line
     * <http://dbpedia.org/resource/Autism> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Disease> .
     */
    public InstanceCheckYago(String path)throws IOException
    {        
        readAllInstances(path);

    }
  
    public boolean isInstance(String name)
    {
        return allInstances_types.keySet().contains(name);
        //return allInstances.contains(name);
    }

      public String[] getTypes(String name)
    {
        return allInstances_types.get(name);
        //return allInstances.contains(name);
    }
/* populates  allInstances_types object
 * key - instance
 * value - array of types
*/
    private void readAllInstances(String path) throws IOException
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
                 if (thisLine.startsWith("#") | !thisLine.contains("rdf:type")) 
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
                     System.err.println("Skipping line " + thisLine);
                     continue;
                 }
                 String subject = thisLine.substring(1, indexOfSubjectEnd);
                 
                 String subjectName = subject;
                 
                 //yago does not use url encoding
                 //subjectName = URLDecoder.decode(subjectName, "UTF-8");
                 
                 int indexOfObjectStart = thisLine.lastIndexOf("<");//indexOfObjectEnd + predicateLength + 3;
                 int indexOfObjectEnd = thisLine.lastIndexOf(">");
                 String objectName = thisLine.substring(indexOfObjectStart+1,indexOfObjectEnd);
                 //yago does not use url encoding
                 //objectName = URLDecoder.decode(objectName, "UTF-8");
                                  
                 

                 String[] types=allInstances_types.get(subjectName);
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
                 allInstances_types.put(subjectName,types);
             }
             }
             catch (java.lang.OutOfMemoryError e)
             {
                 System.err.println(lineCounter);
                 
             }
    }
    
}
