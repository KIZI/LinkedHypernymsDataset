package cz.vse.lhd.lhdtypeinferrer;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tomas
 */
public class THDTypeInferrer {

    private static final HashMap<String, ArrayList<String>> hyperHypo = new HashMap();
    public static Integer maxLines = Integer.MAX_VALUE;

    /**
     * @param input
     * @return
     * @throws java.io.FileNotFoundException
     */
    public static ArrayList<String> readStatFile(String input) throws FileNotFoundException, IOException {
        ArrayList<String> result = new ArrayList();

        FileInputStream fstream = new FileInputStream(input);
        // Get the object of DataInputStream
        DataInputStream in = new DataInputStream(fstream);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String thisLine;
        while ((thisLine = br.readLine()) != null) {
            thisLine = thisLine.trim();
            String thisLineSplit[] = thisLine.split("\t| ");
            if (thisLineSplit.length == 2) {
                if (thisLineSplit[1].contains("/resource/")) {
                    result.add(thisLineSplit[1].replaceAll("^<|>$", ""));
                }

            }

        }
        return result;
    }

    private static void buildHyperHypoHashMap(String filePath) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)))) {
            String line;
            while ((line = br.readLine()) != null) {
                Model model = ModelFactory.createDefaultModel();
                model.read(new ByteArrayInputStream(line.getBytes()), null, "N-TRIPLE");
                if (!model.isEmpty()) {
                    Statement stmt = model.listStatements().next();
                    String hypo = stmt.getSubject().getURI();
                    String hyper = stmt.getObject().asResource().getURI();
                    ArrayList<String> existingHypos = hyperHypo.get(hyper);
                    if (existingHypos == null) {
                        existingHypos = new ArrayList();
                        existingHypos.add(hypo);
                        hyperHypo.put(hyper, existingHypos);
                    } else {
                        existingHypos.add(hypo);
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(THDTypeInferrer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static String getNameFromURI(String uri) {
        int index;

        index = uri.lastIndexOf("/resource/");
        if (index > 0) {
            index = index + "/resource/".length();
        } else {
            index = uri.lastIndexOf("/ontology/");
            index = index + "/ontology/".length();
        }
        return uri.substring(index);
    }

    private static String expandToDBpediaOntologyURI(String name) {
        if (name.equals("owl#Thing")) {
            return "http://www.w3.org/2002/07/owl#Thing";
        } else {
            return "http://dbpedia.org/ontology/" + name;
        }

    }

    public static void completeWithLHD() {
    }

    public static void run(String[] args, Logger logger) throws Exception {

        HashMap<String, String> mappingFromResourcesToClasses = new HashMap();
        String dataPath = Conf.outputDir();
        String resultPath = Conf.outputDir();
        if (!dataPath.endsWith("/")) {
            dataPath = dataPath + "/";
        }
        if (!resultPath.endsWith("/")) {
            resultPath = resultPath + "/";
        }
        BufferedWriter bw;
        String lang = Conf.lang();
        bw = initOutputFile(resultPath + lang + ".inferredmappingstoDBpedia.nt");
        String DBpediaInstancesFilePath = Conf.datasetInstance_typesPath();
        String fileWithEntitiesForMapping = resultPath + lang + ".instances.all.nt";
        String fileWithHypernymsForMapping = resultPath + lang + ".instances.all.stat";
        String fileMappedToDBpedia = resultPath + lang + ".instances.all.inferredmapping.nt";
        String debugPathDBpediaTypes = resultPath + lang + ".debug.inference";
        BufferedWriter bw_debug = initOutputFile(debugPathDBpediaTypes);
        logger.info("Stats loading...");
        ArrayList urisToProcess = readStatFile(fileWithHypernymsForMapping);
        String completeLHD = resultPath + lang + ".instances.all.nt";
        //Ontology omEnglish = new Ontology(dataPath+"dbpedia_3.8.owl" ,lang);
        logger.info("Ontology loading...");
        OntologyCheck ocheck = new OntologyCheck(Conf.datasetOntologyPath());
        logger.info("Instance types loading...");
        InstanceTypesList imList = new InstanceTypesList(DBpediaInstancesFilePath, true);
        logger.info("Hypernyms loading...");
        buildHyperHypoHashMap(completeLHD);
        String result = "";
        logger.info("STI process has been started...");
        for (Iterator<String> it = urisToProcess.iterator(); it.hasNext();) {
            String hypernymURI = it.next();

            //get all instances which has the current hypernym as their type
            ArrayList<String> hypos = hyperHypo.get(hypernymURI);
            HashMap<String, Integer> typeFrequency = new HashMap();
            //for ech instances, which has the current hypernym as its type
            //get its types and add up their frequency
            if (hypos == null) {
                logger.log(Level.WARNING, "No hypos, skipping {0}", hypernymURI);
                continue;
            }
            for (String hypo : hypos) {
                //get all types of this instance
                String[] hypoTypes = imList.getInstanceTypes(hypo);
                if (hypoTypes == null) {
                    //logger.log(Level.WARNING, "No types for {0}", hypo);
                    continue;
                }
                for (String hypoType : hypoTypes) {
                    //imList contains zero-length hyponyms (these replace types which are not in the dbpedia ontology namespace)
                    if ("".equals(hypoType) || !ocheck.isType(expandToDBpediaOntologyURI(hypoType))) {
                        continue;
                    }

                    Integer freq = typeFrequency.get(hypoType);
                    if (freq == null) {
                        typeFrequency.put(hypoType, 1);
                    } else {
                        typeFrequency.put(hypoType, ++freq);
                    }
                }
            }
            // get the most frequent types     

            //System.out.println(hypernymURI);
            // print the frequency for each type
            bw_debug.append("# type \n");
            bw_debug.append(hypernymURI + "\n");
            bw_debug.append("#list of  candidate mapped types,frequency,confidence \n");
            int totalFreq = 0;
            //System.out.println(hypernymURI);
            for (String type : typeFrequency.keySet()) {
                totalFreq = totalFreq + typeFrequency.get(type);
                //System.out.println(type + ":" + typeFrequency.get(type));
            }
            //printMap(sortByComparator(typeFrequency, true), bw_debug, totalFreq);
            //while (at least one discard is made)
            // discard the type if among types of the hypernym there is at least one subtype with frequency,
            //which is not significantly lower (0.8)
            //loop
            // select the type with highest frequency

            boolean discardMade = true;
            while (discardMade) {
                discardMade = false;
                for (String type : typeFrequency.keySet()) {
                    // the constant in the following equation determines the tradeoff between speficity of type and its recall
                    // e.g. value 0.6 means that a more generic type will be removed if there is at least one more specific type
                    // with frequency of at least 60% frequency of the more generic type.
                    float minSubTypeFrequency = typeFrequency.get(type) * 0.2F;

                    //go through other types
                    for (String candidateSubtype : typeFrequency.keySet()) {
                        //among the other types there is also the current type - we skip it
                        if (type.equals(candidateSubtype)) {
                        } //if the other type is a subtype of the current type
                        else if (ocheck.isTransitiveSubtype(expandToDBpediaOntologyURI(type), expandToDBpediaOntologyURI(candidateSubtype))) {
                            if (typeFrequency.get(candidateSubtype) <= minSubTypeFrequency) {
                                //the candidate subtype does not have sufficiently high support
                            } else {
                                //for the current type there is a subclass with similar support
                                // the type can thus be removed
                                //System.out.println("**Removing type " + type + "(" + typeFrequency.get(type) + ") in favour of its subtype " + candidateSubtype + "(" + typeFrequency.get(candidateSubtype) + ")");
                                typeFrequency.remove(type);
                                discardMade = true;
                                break;
                            }
                        }

                    }
                    if (discardMade) {
                        break;
                    }
                }

            }
            //now the types have been pruned so that now they contain either the leaf classes 
            //or non-leaf classes with small support
            // now just selecting the type with highest support
            int maxFreq = -1;
            String maxFreqType = "";
            int maxFrequencyofMaxFreqType = -1;
            bw_debug.append("#Pruned set of types,frequency,confidence \n");
            //printMap(sortByComparator(typeFrequency, true), bw_debug, totalFreq);
            for (String type : typeFrequency.keySet()) {
                int curFreq = typeFrequency.get(type);

                if (curFreq > maxFreq) {
                    maxFreq = curFreq;
                    maxFreqType = type;
                    maxFrequencyofMaxFreqType = curFreq;
                }
            }
            if (maxFreq == -1) {
                continue;
            }
            //System.out.println("xxxxx");
            //System.out.println(hypernymURI);
            //System.out.println(maxFreqType);
            //System.out.println(maxFreq);
            bw_debug.append("#Selected mapping, confidence\n");
            float confidence = ((float) maxFrequencyofMaxFreqType) / ((float) totalFreq);
            bw_debug.append(maxFreqType + "," + confidence + " \n\n");
            mappingFromResourcesToClasses.put(hypernymURI, maxFreqType);
            result = result + "#" + maxFreq + "\n";
            result = result + "<" + hypernymURI + "> <http://www.w3.org/2000/01/rdf-schema#subClassOf> <http://dbpedia.org/ontology/" + maxFreqType + "> .\n";
            //System.out.println("<http://dbpedia.org/resource/Stagename> <http://www.w3.org/2000/01/rdf-schema#subClassOf> <http://dbpedia.org/ontology/Name>".
            //System.out.println("xxxxx");
        }

        bw.append(result);
        bw.close();
        bw_debug.close();
        translateTypesFromResourcesToClasses(fileWithEntitiesForMapping, fileMappedToDBpedia, mappingFromResourcesToClasses, imList);
//        hypernym  h in instances.all.dbpedia.resource.stat
//{
//  ArrayList typeList ={}
//  for each entry e in instances-all.nt which has hypernym h
//   {
//   lowestGranularityTypes = {}
//   for each  type t of entry e in instance_types_en.nt
//    {
//      if t is a superclass according to dbpedia_3.8.owl of any x \in
//lowestGranularityTypes
//        {
//        continue;
//        }
//      add t to typeList if not already there and increment frequency
//of t in typeList by 1
//    }
//  select t with highest frequency from typeList
//  }
//}
//
//kde
//instances.all.dbpedia.resource.stat - všechny typy nenamapovaných
//        
    }

    private static Map<String, Integer> sortByComparator(Map<String, Integer> unsortMap, final boolean order) {

        List<Entry<String, Integer>> list = new LinkedList<>(unsortMap.entrySet());

        // Sorting the list based on values
        Collections.sort(list, new Comparator<Entry<String, Integer>>() {
            @Override
            public int compare(Entry<String, Integer> o1,
                    Entry<String, Integer> o2) {
                if (order) {
                    return o1.getValue().compareTo(o2.getValue());
                } else {
                    return o2.getValue().compareTo(o1.getValue());

                }
            }
        });

        // Maintaining insertion order with the help of LinkedList
        Map<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
        for (Entry<String, Integer> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

    public static void printMap(Map<String, Integer> map, BufferedWriter bw, int totalFreq) throws IOException {
        for (String type : map.keySet()) {
            int freq = map.get(type);
            bw.append(type + "," + map.get(type) + "," + ((float) freq / (float) totalFreq) + "\n");
            System.out.println(type + ":" + map.get(type));
        }
    }

    public static BufferedWriter initOutputFile(String path) throws IOException {
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        file.createNewFile();
        FileWriter file_fw = new FileWriter(file.getAbsoluteFile(), false);

        return new BufferedWriter(file_fw);

    }

    public static void translateTypesFromResourcesToClasses(String path, String outputPath, HashMap<String, String> mappingFromResourcesToClasses, InstanceTypesList imList) throws IOException {
//                    Pattern classPattern  = Pattern.compile("<owl:Class rdf:about=\"([^\"]+)\">(.*?)</owl:Class>", Pattern.DOTALL);    

        FileInputStream fstream = new FileInputStream(path);
        // Get the object of DataInputStream
        String pathToOnlyInstancesWhereInferredMappingWasApplied_NOTYPEINDBPEDIAINSTANCEFILE = outputPath.replace(".nt", "") + ".onlyinferred.notdbpediainstance.nt";
        String pathToOnlyInstancesWhereInferredMappingWasApplied_SAMETYPEINDBPEDIAINSTANCEFILE = outputPath.replace(".nt", "") + ".onlyinferred.overlapswithdbpediaassignedtype.nt";
        String pathToOnlyInstancesWhereInferredMappingWasApplied_DIFFERENTTYPEINDBPEDIAINSTANCEFILE = outputPath.replace(".nt", "") + ".onlyinferred.otherdbpediaassignedtype.nt";
        DataInputStream in = new DataInputStream(fstream);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        StringBuilder all = new StringBuilder();
        String thisLine;

        BufferedWriter bw_NOTYPEINDBPEDIAINSTANCEFILE;
        BufferedWriter bw_SAMETYPEINDBPEDIAINSTANCEFILE;
        BufferedWriter bw_DIFFERENTTYPEINDBPEDIAINSTANCEFILE;
        try (BufferedWriter bw = THDTypeInferrer.initOutputFile(outputPath)) {

            bw_NOTYPEINDBPEDIAINSTANCEFILE = THDTypeInferrer.initOutputFile(pathToOnlyInstancesWhereInferredMappingWasApplied_NOTYPEINDBPEDIAINSTANCEFILE);
            bw_SAMETYPEINDBPEDIAINSTANCEFILE = THDTypeInferrer.initOutputFile(pathToOnlyInstancesWhereInferredMappingWasApplied_SAMETYPEINDBPEDIAINSTANCEFILE);
            bw_DIFFERENTTYPEINDBPEDIAINSTANCEFILE = THDTypeInferrer.initOutputFile(pathToOnlyInstancesWhereInferredMappingWasApplied_DIFFERENTTYPEINDBPEDIAINSTANCEFILE);
            int lineCounter = 0;
            try {

                while ((thisLine = br.readLine()) != null) {
                    lineCounter++;
                    if (thisLine.startsWith("#")) {
                        continue;
                    }

                    int indexOfSubjectEnd = thisLine.indexOf(">");
                    if (indexOfSubjectEnd < 0) {
                        System.err.println("Skipping line " + thisLine);
                        continue;
                    }
                    String subject = thisLine.substring(1, indexOfSubjectEnd);

                    String subjectName = subject;

                    //yago does not use url encoding
                    //subjectName = URLDecoder.decode(subjectName, "UTF-8");
                    int indexOfObjectStart = thisLine.lastIndexOf("<");//indexOfObjectEnd + predicateLength + 3;
                    int indexOfObjectEnd = thisLine.lastIndexOf(">");
                    String objectName = thisLine.substring(indexOfObjectStart + 1, indexOfObjectEnd);

                    String mapping = mappingFromResourcesToClasses.get(objectName);
                    String outputLine;

                    if (mapping != null) {
                        outputLine = thisLine.replace(objectName + "> .", "http://dbpedia.org/ontology/" + mapping + "> .");
                        if (imList.isInstance(getNameFromURI(subjectName))) {
                            if (imList.isInstanceType(getNameFromURI(subjectName), mapping)) {
                                bw_SAMETYPEINDBPEDIAINSTANCEFILE.write(outputLine + "\n");
                            } else {
                                bw_DIFFERENTTYPEINDBPEDIAINSTANCEFILE.write(outputLine + "\n");
                            }
                        } else {
                            bw_NOTYPEINDBPEDIAINSTANCEFILE.write(outputLine + "\n");
                        }
                    } else {
                        outputLine = thisLine;
                    }
                    bw.write(outputLine + "\n");

                }
            } catch (java.lang.OutOfMemoryError e) {
                System.err.println(lineCounter);

            }
        }
        bw_NOTYPEINDBPEDIAINSTANCEFILE.close();
        bw_DIFFERENTTYPEINDBPEDIAINSTANCEFILE.close();
        bw_SAMETYPEINDBPEDIAINSTANCEFILE.close();
    }
}
