package cz.vse.lhd.lhdontologycleanup;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tomas
 */
public class InstanceCheck {
    //private static HashSet allInstances = new HashSet();

    private HashMap<String, String[]> allInstances_types = new HashMap();
    HashSet exceptions = new HashSet();
    //private static int predicateLength = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>".length();
    private boolean onlyDBpediantologyTypes = false;
    /*
     * example input line
     * <http://dbpedia.org/resource/Autism> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Disease> .
     */

    public InstanceCheck(String path, String exceptionsFile) throws IOException {

        readAllInstances(path);
        if (exceptionsFile != null) {
            readExceptions(exceptionsFile);
            removeExceptions();
        }

    }
    /*
     * for example, if an entity is an instance of dbpedia-owl:Species, 
     * it is not considered as an instance
     */

    private void removeExceptions() {
        ArrayList<String> toRemove = new ArrayList();
        for (Entry<String, String[]> entry : allInstances_types.entrySet()) {
            String key = entry.getKey();
            for (String assignedType : entry.getValue()) {
                if (exceptions.contains(assignedType)) {
                    toRemove.add(key);
                    break;
                }
            }

        }
        for (String key : toRemove) {
            allInstances_types.remove(key);
        }
        System.out.println("Removed " + toRemove.size() + " instances based on the exceptions file");
    }

    public boolean isInstance(String name) {
        return allInstances_types.keySet().contains(name);
        //return allInstances.contains(name);
    }

    public boolean isInstanceType(String instanceName, String instanceType) {
        String[] instanceTypes = allInstances_types.get(instanceName);
        if (instanceTypes == null) {
            return false;
        }
        for (String type : instanceTypes) {
            if (type.equals(instanceType)) {
                return true;
            }
        }
        return false;
        //return allInstances.contains(name);
    }

    private void readExceptions(String path) {
        FileInputStream fstream = null;
        try {
            fstream = new FileInputStream(path);
            // Get the object of DataInputStream
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            StringBuilder all = new StringBuilder();
            String thisLine;
            int lineCounter = 0;
            while ((thisLine = br.readLine()) != null) {
                lineCounter++;
                if (thisLine.startsWith("#")) {
                    continue;
                } else {
                    String conceptName = THDOntologyCleanup.getConceptName(thisLine);
                    exceptions.add(conceptName);
                }
            }
        } catch (IOException ex) {
        } finally {
            try {
                if (fstream != null) {
                    fstream.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(InstanceCheck.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    private void readAllInstances(String path) throws IOException {
//                    Pattern classPattern  = Pattern.compile("<owl:Class rdf:about=\"([^\"]+)\">(.*?)</owl:Class>", Pattern.DOTALL);    

        FileInputStream fstream = new FileInputStream(path);
        // Get the object of DataInputStream
        DataInputStream in = new DataInputStream(fstream);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        StringBuilder all = new StringBuilder();
        String thisLine;
        int lineCounter = 0;
        try {


            while ((thisLine = br.readLine()) != null) {
                lineCounter++;
                if (thisLine.startsWith("#")) {
                    continue;
                }
                if (lineCounter > THDOntologyCleanup.maxLines) {
                    System.out.println("Reached maxLines, quitting InstanceCheck.readAllInstances");
                    break;
                }


//to save memory only the concept name is stored, not full uri            
                int indexOfSubjectEnd = thisLine.indexOf(" ");
                String subject = thisLine.substring(0, indexOfSubjectEnd - 1);

                String subjectName = subject.substring(subject.lastIndexOf("/") + 1);

                subjectName = URLDecoder.decode(subjectName, "UTF-8");

                int indexOfObjectStart = thisLine.lastIndexOf("/");//indexOfObjectEnd + predicateLength + 3;
                int indexOfObjectEnd = thisLine.lastIndexOf(">");
                String objectName;
                if (onlyDBpediantologyTypes && !thisLine.contains("/dbpedia.org/ontology")) {
                    //use empty objectName rather than skip, 
                    // because the list of keys is used to check whether the article is an instance or not
                    objectName = "";
                } else {
                    objectName = thisLine.substring(indexOfObjectStart + 1, indexOfObjectEnd);
                }



                String[] types = allInstances_types.get(subjectName);
                if (types == null) {
                    types = new String[1];
                    types[0] = objectName;
                } else {
                    String newTypes[] = new String[types.length + 1];
                    for (int i = 0; i < types.length; i++) {
                        newTypes[i] = types[i];
                    }
                    newTypes[newTypes.length - 1] = objectName;
                    types = newTypes;
                }
                allInstances_types.put(subjectName, types);
            }
        } catch (java.lang.OutOfMemoryError e) {
            System.err.println(lineCounter);

        }
    }
}
