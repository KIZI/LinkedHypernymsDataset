package cz.vse.lhd.lhdtypeinferrer;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;

/**
 *
 * @author tomas
 */
public class OntologyCheck {

    //key = class ,  value list of  superclass
    private final HashMap<String, String[]> allITaxonomy = new HashMap();
    /*
     * example input line
     * <http://dbpedia.org/resource/Autism> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Disease> .
     */

    public OntologyCheck(String path) throws IOException {
        readAllITaxonomy(path);
    }

    public boolean isType(String name) {
        return allITaxonomy.containsKey(name);
    }

    public boolean isTransitiveSubtype(String superType, String subType) {
        String[] ontologySuperTypes = allITaxonomy.get(subType);
        //first check if one of the direct supertypes matches
        if (ontologySuperTypes == null) {
            return false;
        }

        for (String ontologySuperType : ontologySuperTypes) {
            if (superType.equals(ontologySuperType)) {
                return true;
            }
        }
        //second, check if one of the  supertypes matches transitively
        for (String ontologySuperType : ontologySuperTypes) {
            if (isTransitiveSubtype(superType, ontologySuperType)) {
                return true;
            }
        }
        return false;
    }

    public String[] getDirectSupertypes(String type) {
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

    private void readAllITaxonomy(String path) throws IOException {
//                    Pattern classPattern  = Pattern.compile("<owl:Class rdf:about=\"([^\"]+)\">(.*?)</owl:Class>", Pattern.DOTALL);
        Model model = ModelFactory.createDefaultModel();
        model.read(new FileInputStream(Conf.datasetOntologyPath()), null, "RDF/XML");
        StringWriter n3 = new StringWriter();
        model.write(n3, "N-TRIPLE");
        n3.flush();

        BufferedReader br = new BufferedReader(new StringReader(n3.toString()));
        String thisLine;
        int lineCounter = 0;
        try {

            while ((thisLine = br.readLine()) != null) {
                lineCounter++;
                if (thisLine.startsWith("#") | !(thisLine.contains("rdfs:subClassOf") | thisLine.contains("http://www.w3.org/2000/01/rdf-schema#subClassOf"))) {
                    continue;
                }
                if (lineCounter > THDTypeInferrer.maxLines) {
                    System.out.println("Reached maxLines, quitting InstanceCheck.readAllInstances");
                    break;
                }

                int indexOfSubjectEnd = thisLine.indexOf(">");
                if (indexOfSubjectEnd < 0) {
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
                String objectName = thisLine.substring(indexOfObjectStart + 1, indexOfObjectEnd);
                // objectName=URLDecoder.decode(objectName, "UTF-8");
                String[] types = allITaxonomy.get(subjectName);
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
                allITaxonomy.put(subjectName, types);
            }
        } catch (java.lang.OutOfMemoryError e) {
            System.err.println(lineCounter);

        }
    }
}
