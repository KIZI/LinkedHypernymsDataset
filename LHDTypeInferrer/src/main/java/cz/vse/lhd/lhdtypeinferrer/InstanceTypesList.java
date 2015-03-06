package cz.vse.lhd.lhdtypeinferrer;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tomas
 */
public class InstanceTypesList {
    //private static HashSet allInstances = new HashSet();

    private final HashMap<String, String[]> allInstances_types = new HashMap();
    //private static int predicateLength = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>".length();
    private boolean onlyDBpediantologyTypes = false;
    /*
     * example input line
     * <http://dbpedia.org/resource/Autism> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Disease> .
     */

    public InstanceTypesList(String path, boolean onlyDBpediantologyTypes) throws IOException {
        this.onlyDBpediantologyTypes = onlyDBpediantologyTypes;
        readAllInstances(path);
    }

    public boolean isInstance(String name) {
        return allInstances_types.keySet().contains(name);
        //return allInstances.contains(name);
    }

    public String[] getInstanceTypes(String instanceName) {

        int trimIndex = instanceName.indexOf("dbpedia.org/resource/");
        if (trimIndex > -1) {
            instanceName = instanceName.substring(trimIndex + "dbpedia.org/resource/".length());
        }
        return allInstances_types.get(instanceName);

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

    private void readAllInstances(String path) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path)))) {
            String line;
            while ((line = br.readLine()) != null) {
                if ((onlyDBpediantologyTypes && line.contains("/dbpedia.org/ontology")) || !onlyDBpediantologyTypes) {
                    Model model = ModelFactory.createDefaultModel();
                    model.read(new ByteArrayInputStream(line.getBytes()), null, "N-TRIPLE");
                    if (!model.isEmpty()) {
                        Statement stmt = model.listStatements().next();
                        String subjectName = URLDecoder.decode(stmt.getSubject().getURI().replaceFirst(".+?dbpedia.org/resource/", ""), "UTF-8");
                        String objectName = URLDecoder.decode(stmt.getObject().asResource().getURI().replaceFirst(".+?dbpedia.org/ontology/", ""), "UTF-8");
                        String[] types = allInstances_types.get(subjectName);
                        if (types == null) {
                            types = new String[1];
                            types[0] = objectName;
                        } else {
                            String newTypes[] = new String[types.length + 1];
                            System.arraycopy(types, 0, newTypes, 0, types.length);
                            newTypes[newTypes.length - 1] = objectName;
                            types = newTypes;
                        }
                        allInstances_types.put(subjectName, types);
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(THDTypeInferrer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
