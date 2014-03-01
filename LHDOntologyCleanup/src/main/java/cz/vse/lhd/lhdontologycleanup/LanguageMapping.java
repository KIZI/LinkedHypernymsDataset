package cz.vse.lhd.lhdontologycleanup;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

/**
 *
 * @author tomas
 */
public class LanguageMapping {
    private HashMap<String,String> localizedDBpediaToEnglishDBpedia= new HashMap();
    private String lang;
    public  LanguageMapping(String pathToSameAsFile, String _lang) throws IOException
    {
        lang  = _lang;
        readMappings(pathToSameAsFile,lang);
    }
    
    public  String getEnglishName(String localizedName)
    {
        return localizedDBpediaToEnglishDBpedia.get(localizedName);
        
    }
    private  void readMappings(String path, String lang) throws IOException
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
                 if (thisLine.startsWith("#")) 
                 {
                     continue;
                 }
                 if (!thisLine.contains("http://" + lang + ".dbpedia.org"))
                 {
                     continue;
                 }
                 if (lineCounter > THDOntologyCleanup.maxLines)
                 {
                     System.out.println("Reached maxLines, quitting LanguageMapping.readMappings" );
                     break;
                 }

//to save memory only the concept name is stored, not full uri            
                 int indexOfSubjectEnd=thisLine.indexOf(" ");
                 String subject = thisLine.substring(0, indexOfSubjectEnd-1);
                 String subjectName = subject.substring(subject.lastIndexOf("/")+1);
                 int indexOfObjectStart = thisLine.lastIndexOf("/");//indexOfObjectEnd + predicateLength + 3;
                 int indexOfObjectEnd = thisLine.lastIndexOf(">");
                 String objectName = thisLine.substring(indexOfObjectStart+1,indexOfObjectEnd);
                

                 localizedDBpediaToEnglishDBpedia.put(objectName,subjectName);
             }
             }
             catch (java.lang.OutOfMemoryError e)
             {
                 System.err.println(lineCounter);
                 
             }
    }
}
