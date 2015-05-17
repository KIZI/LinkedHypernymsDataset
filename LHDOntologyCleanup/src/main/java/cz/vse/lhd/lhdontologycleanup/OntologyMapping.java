package cz.vse.lhd.lhdontologycleanup;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author tomas
 */
public class OntologyMapping {

    private HashMap<String, ArrayList<String>> nameClassMappings = new HashMap();
    private final static String subclassPredicate = "http://www.w3.org/2000/01/rdf-schema#subClassOf";
    private String lang;

    public OntologyMapping(String ontologyPath, String supplementalOntologyPath, String _lang) throws IOException {
        lang = _lang;
        readOntologyTypes(ontologyPath, lang);
        //experimental -  does not work
        readSupplementalOntologyTypes(supplementalOntologyPath);

    }

    private String getDBpediaBaseURI() {

        if (lang.equals("en")) {
            return "http://dbpedia.org/resource/";
        } else {
            return "http://" + lang + ".dbpedia.org/resource/";
        }
    }

    public String map_sameAs(String uri) {
        String name = uri.substring(getDBpediaBaseURI().length()).toLowerCase();
        name = name.replace("_", " ");
        ArrayList<String> possibleClasses = nameClassMappings.get(name);

        if (possibleClasses == null) {
            return null;
        } else {
            return selectClass(name, possibleClasses);
        }
    }

    private String selectClass(String conceptName, ArrayList<String> possibleClasses) {
        ArrayList<String> uniqueClasses = new ArrayList();
        for (String possibleClass : possibleClasses) {
            if (!uniqueClasses.contains(possibleClass)) {
                uniqueClasses.add(possibleClass);
            }
        }
        if (uniqueClasses.size() == 1) {
            return uniqueClasses.get(0);
        } else {
            //TODO: prefer class from supplemental ontology
            System.out.println("Arbitrarily selecting from " + uniqueClasses.size() + " candidates");
            return uniqueClasses.get(0);
        }
    }
    /*
     * returns the most precise subclass  for argument
     * subclass is identified as class with longer name, containing the name of the class in the argument as substring
     * most precise = longest name, the subclass name must end with the argument
     */

    public String map_subclass(String uri) {
        String bestMatchingName = null;

        int bestMatchingNameLength = Integer.MIN_VALUE;
        String name = uri.substring(getDBpediaBaseURI().length()).toLowerCase();
        for (String candidateName : nameClassMappings.keySet()) {
            if (candidateName.matches(".*\\b" + name + "$") && candidateName.length() > name.length()) {
                int curLength = candidateName.length();
                /* rightmost matches are preferred
                 * e.g. in LawFirm  Firm is preferred before Law
                 */
                if (curLength > bestMatchingNameLength) {
                    bestMatchingName = candidateName;
                    bestMatchingNameLength = curLength;
                }
            }
        }
        if (bestMatchingName != null) {
            return selectClass(bestMatchingName, nameClassMappings.get(bestMatchingName));
        } else {
            return null;
        }
    }

    /*
     * returns the most precise superclass  for argument
     * superclass is identified as class with shorter name, contained in the name of the class in the argument as substring
     * most precise = longest of matching
     */
    public String map_superclass(String uri) {
        String bestMatchingName = null;

        int bestMatchingNameLength = Integer.MIN_VALUE;
        String name = uri.substring(getDBpediaBaseURI().length()).toLowerCase();
        for (String candidateName : nameClassMappings.keySet()) {
            if (name.matches(".*" + candidateName + "s?$") && candidateName.length() < name.length()) {
                int curLength = candidateName.length();
                /* rightmost matches are preferred
                 * e.g. in LawFirm  Firm is preferred before Law
                 */
                if (curLength > bestMatchingNameLength) {
                    bestMatchingName = candidateName;
                    bestMatchingNameLength = curLength;
                }
            }
        }
        if (bestMatchingName != null) {
            return selectClass(bestMatchingName, nameClassMappings.get(bestMatchingName));
        } else {
            return null;
        }
    }

    /*
     * Example input dbpedia_3.8.owl
     *    <owl:Class rdf:about="http://dbpedia.org/ontology/BasketballLeague">
     <rdfs:label xml:lang="en">basketball league</rdfs:label><rdfs:label xml:lang="el">Ομοσπονδία Καλαθοσφαίρισης</rdfs:label><rdfs:label xml:lang="fr">ligue de basketball</rdfs:label><rdfs:comment xml:lang="en">a group of sports teams that compete against each other in Basketball</rdfs:comment><rdfs:subClassOf rdf:resource="http://dbpedia.org/ontology/SportsLeague"></rdfs:subClassOf>
     </owl:Class><owl:Class rdf:about="http://dbpedia.org/ontology/LunarCrater">
     <rdfs:label xml:lang="en">lunar crater</rdfs:label><rdfs:label xml:lang="fr">cratère lunaire</rdfs:label><rdfs:label xml:lang="el">Σεληνιακός κρατήρας</rdfs:label><rdfs:subClassOf rdf:resource="http://dbpedia.org/ontology/NaturalPlace"></rdfs:subClassOf>
     </owl:Class>
     */
    private void readOntologyTypes(String ontologyPath, String lang) throws IOException {
        Pattern classPattern = Pattern.compile("<owl:Class rdf:about=\"([^\"]+)\">(.*?)</owl:Class>", Pattern.DOTALL);

        FileInputStream fstream = new FileInputStream(ontologyPath);
        // Get the object of DataInputStream
        DataInputStream in = new DataInputStream(fstream);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        StringBuilder all = new StringBuilder();
        String thisLine;
        while ((thisLine = br.readLine()) != null) {
            all.append(thisLine);
        }
        Matcher classMatcher = classPattern.matcher(all);

        while (classMatcher.find()) {
            String className = classMatcher.group(1);
            //there are no alternative names for nl in dbpedia_3.8.owl, but many for de
            ArrayList<String> altNames = getAlternativeNames(classMatcher.group(2), lang);

            for (String altName : altNames) {
                ArrayList<String> nameClassMapping = nameClassMappings.get(altName);
                if (nameClassMapping == null) {
                    nameClassMapping = new ArrayList();
                    nameClassMapping.add(className);
                    nameClassMappings.put(altName, nameClassMapping);
                } else {
                    nameClassMapping.add(className);
                }
            }
        }
    }

    /*
     * Example input in nt syntax
     <http://dbpedia.org/resource/Display> <http://www.w3.org/2000/01/rdf-schema#subClassOf> <http://dbpedia.org/ontology/Play> .
     <http://dbpedia.org/resource/Boxers> <http://www.w3.org/2000/01/rdf-schema#subClassOf> <http://dbpedia.org/ontology/Boxer> .

     */
    private void readSupplementalOntologyTypes(String ontologyPath) {
        FileInputStream fstream = null;
        try {
            fstream = new FileInputStream(ontologyPath);
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
                }
                if (!thisLine.contains(subclassPredicate)) {
                    continue;
                }
                int indexOfSubjectEnd = thisLine.indexOf(" ");
                String subject = thisLine.substring(1, indexOfSubjectEnd - 1);
                String subjectName = subject.substring(subject.lastIndexOf("/") + 1);
                /*                 int indexOfObjectNameStart = thisLine.lastIndexOf("/");//indexOfObjectEnd + predicateLength + 3;
                 int indexOfObjectStart = thisLine.lastIndexOf("<");
                 int indexOfObjectEnd = thisLine.lastIndexOf(">");
                 String objectName = thisLine.substring(indexOfObjectNameStart+1,indexOfObjectEnd);
                 String object = thisLine.substring(indexOfObjectStart+1,indexOfObjectEnd);*/

                ArrayList<String> nameClassMapping = nameClassMappings.get(subjectName);
                if (nameClassMapping == null) {
                    nameClassMapping = new ArrayList();
                    nameClassMapping.add(subject);
                    nameClassMappings.put(subjectName.toLowerCase(), nameClassMapping);
                } else {
                    System.out.println("Mapping for " + subjectName + " already loaded from the primary ontology");
                }

            }
        } catch (IOException ex) {
        } finally {
            try {
                if (fstream != null) {
                    fstream.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(OntologyMapping.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    /*
     * example input
     *  <rdfs:label xml:lang="en">basketball league</rdfs:label><rdfs:label xml:lang="el">Ομοσπονδία Καλαθοσφαίρισης</rdfs:label><rdfs:label xml:lang="fr">ligue de basketball</rdfs:label><rdfs:comment xml:lang="en">a group of sports teams that compete against each other in Basketball</rdfs:comment><rdfs:subClassOf rdf:resource="http://dbpedia.org/ontology/SportsLeague"></rdfs:subClassOf>
     */
    private ArrayList<String> getAlternativeNames(String classElemetContent, String lang) {
        Pattern alternativeNamesPattern = Pattern.compile("<rdfs:label xml:lang=\"" + lang + "\">(.*?)</rdfs:label>", Pattern.DOTALL);
        Matcher classMatcher = alternativeNamesPattern.matcher(classElemetContent);

        ArrayList<String> altNames = new ArrayList();
        while (classMatcher.find()) {
            altNames.add(classMatcher.group(1).toLowerCase());

        }
        return altNames;
    }
}
