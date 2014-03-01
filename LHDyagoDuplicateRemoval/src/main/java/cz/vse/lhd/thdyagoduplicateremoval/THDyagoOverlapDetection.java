package cz.vse.lhd.thdyagoduplicateremoval;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author tomas
 */
public class THDyagoOverlapDetection {

    public static int maxLines = Integer.MAX_VALUE;
    private static HashMap<String, ArrayList<String>> hyperHypo = new HashMap();
    private static HashMap<String, String> hypoHyper = new HashMap();
    private static BufferedWriter output_file_yago_notypebefore;
    private static BufferedWriter output_file_yago_exactmatch;
    private static BufferedWriter output_file_yago_approxmatch;
    private static BufferedWriter output_file_yago_nomatch;
    private static ArrayList<String> allFilePathToGenerateStatsFrom = new ArrayList();
    private static final String typePredicate = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

    /**
     * @param args the command line arguments
     *
     * If run without params, hardcoded paths are used. In this mode, the number
     * of processed lines of Yago is limited, which is useful for debugging in a
     * workstation with limited memory.
     *
     * When run with parameters, the line limit is increased to infinity
     */
    public static void main(String[] args) throws IOException {
        String basePath = Conf.outputDir();
        String lang = Conf.lang();
        String inputTHDfile = lang + ".instances.mapped.existing.nt";
        if (args.length != 2) {
            System.out.println("argument 1 'Base path' not provided, using default=" + basePath);
            //System.out.println("argument 2 'language' not provided, using default="+lang);
            System.out.println("argument 2 'input THD file' not provided, using default=" + inputTHDfile);
//            System.out.println("Running in the desktop mode (limiting the number of processed lines to avoid out of memory)");
//            maxLines = 1000000;
        } else {
            basePath = args[0];
            inputTHDfile = args[1];
            //lang=args[1];
        }
        if (!basePath.endsWith("/")) {
            basePath = basePath + "/";
        }
        // SET PATHS
        System.out.println("thdOntologyCleanupResult=" + basePath);
        String ontologyPath = Conf.datasetYagoTaxonomyPath();
        System.out.println("ontologyPath=" + ontologyPath);
        String YAGOInstancesFilePath = Conf.datasetYagoTypesPath();
        System.out.println("YAGOInstancesFilePath=" + YAGOInstancesFilePath);
        String thdInputPath = basePath + inputTHDfile;//lang + ".instances.notmapped.new.nt"; //, thdOntologyCleanupResultBasePath + lang + ".instances.mapped.new.notypebefore.nt", thdOntologyCleanupResultBasePath + lang + "en.instances.mapped.new.nt" } ;        
        System.out.println("thdInputPath=" + thdInputPath);

        initOutputFiles(basePath + inputTHDfile.replaceAll("\\.nt", ""));

        InstanceCheckYago yagoInstances = new InstanceCheckYago(YAGOInstancesFilePath);
        OntologyCheckYago yagoOntology = new OntologyCheckYago(ontologyPath);
        buildHyperHypoHashMap(thdInputPath);
        YagoTaxonomyMatcher ytm = new YagoTaxonomyMatcher(yagoInstances, yagoOntology);
        detectYAGOoverlap(ytm);
        for (String path : allFilePathToGenerateStatsFrom) {
            generateFileWithListOfTypesAndFrequency(path);
        }
    }

    public static void generateFileWithListOfTypesAndFrequency(String input) throws FileNotFoundException, IOException {
        HashMap<String, Integer> all = new HashMap();

        FileInputStream fstream = new FileInputStream(input);
        String resultPath = input.replace(".nt", ".stat");
        BufferedWriter resWriter = initOutputFile(resultPath, false);
        // Get the object of DataInputStream
        DataInputStream in = new DataInputStream(fstream);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String thisLine;
        String lineEnd = "";
        while ((thisLine = br.readLine()) != null) {
            if (thisLine.startsWith("#")) {
                continue;
            }
            int indexOfObjectEnd = thisLine.lastIndexOf(">");
            int indexOfObjectStart = thisLine.lastIndexOf("<");
            String objectName = thisLine.substring(indexOfObjectStart + 1, indexOfObjectEnd);
            Integer count = all.get(objectName);

            if (count == null) {
                all.put(objectName, 1);
            } else {
                all.put(objectName, ++count);
            }

        }
        Boolean DESC = false;
        Map<String, Integer> sortedMap = SortUtils.sortByComparator(all, DESC);
        for (Map.Entry<String, Integer> entry : sortedMap.entrySet()) {
            resWriter.append(entry.getValue() + "\t" + entry.getKey() + "\n");
        }
        System.gc();
        resWriter.close();

    }

    public static String getConceptName(String uri) {
        return uri.substring(uri.lastIndexOf("/") + 1);
    }

    private static String urlEncodeResource(String uri) throws UnsupportedEncodingException {
        int indexoflastSlash = uri.lastIndexOf("/");
        String lastPart = uri.substring(indexoflastSlash + 1);
        String firstPart = uri.substring(0, indexoflastSlash + 1);
        return firstPart + URLEncoder.encode(lastPart, "UTF-8");

    }

    private static void saveTriple(String subject, String predicate, String object, BufferedWriter bw) throws IOException {
        try {
            bw.append("<" + urlEncodeResource(subject) + "> <" + predicate + "> <" + urlEncodeResource(object) + "> .\n");
        } catch (UnsupportedEncodingException ex) {
            System.out.println(ex);
        }
    }

    private static void detectYAGOoverlap(YagoTaxonomyMatcher ytm) throws IOException {
        for (String hypo : hypoHyper.keySet()) {
            String hyper = hypoHyper.get(hypo);
            String hypo_name = getConceptName(hypo);
            String hyper_name = getConceptName(hyper);
            if (!ytm.isInstance(hypo_name)) {
                saveTriple(hypo, typePredicate, hypoHyper.get(hypo), output_file_yago_notypebefore);
                //output_file_yago_notypebefore.append("<" + hypo + "> <" + typePredicate + "> <" + hyper + "> .\n");

            } else {
                MatchType detected;
                String matchResult_approx = ytm.isInstanceType(hypo_name, hyper_name, MatchType.approx);
                if (matchResult_approx != null) {
                    String matchResult_exact = ytm.isInstanceType(hypo_name, hyper_name, MatchType.exact);
                    if (matchResult_exact != null) {
                        //exact match
                        output_file_yago_exactmatch.append("# " + matchResult_exact + "\n");
                        saveTriple(hypo, typePredicate, hyper, output_file_yago_exactmatch);
                        //output_file_yago_exactmatch.append("<" + hypo + "> <" + typePredicate + "> <" + hyper + ">.\n");
                    } else {
                        saveTriple(hypo, typePredicate, hyper, output_file_yago_approxmatch);
                        output_file_yago_approxmatch.append("# " + matchResult_approx + "\n");
                        //output_file_yago_approxmatch.append("<" + hypo + "> <" + typePredicate + "> <" + hyper + "> .\n");
                        //approx match
                    }
                } else {
                    saveTriple(hypo, typePredicate, hyper, output_file_yago_nomatch);
                    //output_file_yago_nomatch.append("<" + hypo + "> <" + typePredicate + "> <" + hyper + "> .\n");
                }
            }
        }
        output_file_yago_notypebefore.close();
        output_file_yago_exactmatch.close();
        output_file_yago_approxmatch.close();
        output_file_yago_nomatch.close();

    }

    private static void buildHyperHypoHashMap(String filePath) {


        try {
            FileInputStream fstream = new FileInputStream(filePath);
            // Get the object of DataInputStream
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String thisLine;
            while ((thisLine = br.readLine()) != null) {
                if (thisLine.startsWith("#")) {
                    continue;
                }


                int indexOfSubjectStart = thisLine.indexOf("<");
                int indexOfSubjectEnd = thisLine.indexOf(">");

                String subject = thisLine.substring(indexOfSubjectStart + 1, indexOfSubjectEnd);
                int indexOfObjectStart = thisLine.lastIndexOf("<");
                int indexOfObjectEnd = thisLine.lastIndexOf(">");
                String object = thisLine.substring(indexOfObjectStart + 1, indexOfObjectEnd);


                String hypo = URLDecoder.decode(subject, "UTF-8");
                String hyper = URLDecoder.decode(object, "UTF-8");
                ArrayList<String> existingHypos = hyperHypo.get(hyper);
                if (existingHypos == null) {
                    existingHypos = new ArrayList();
                    existingHypos.add(hypo);
                    hyperHypo.put(hyper, existingHypos);
                } else {
                    existingHypos.add(hypo);
                }
                hypoHyper.put(hypo, hyper);
            }

        } catch (IOException ex) {
        }
    }

    private static void initOutputFiles(String path) throws IOException {
        String basePath = path;
        output_file_yago_notypebefore = initOutputFile(basePath + ".yago_notypebefore.nt", true);
        output_file_yago_exactmatch = initOutputFile(basePath + ".yago_exactmatch.nt", true);
        output_file_yago_approxmatch = initOutputFile(basePath + ".yago_approxmatch.nt", true);
        output_file_yago_nomatch = initOutputFile(basePath + ".yago_nomatch.nt", true);
    }

    private static BufferedWriter initOutputFile(String path, boolean includeOnGenerateStatsList) throws IOException {
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        file.createNewFile();
        FileWriter file_fw = new FileWriter(file.getAbsoluteFile(), false);
        if (includeOnGenerateStatsList) {
            allFilePathToGenerateStatsFrom.add(path);
        }
        return new BufferedWriter(file_fw);

    }
}
