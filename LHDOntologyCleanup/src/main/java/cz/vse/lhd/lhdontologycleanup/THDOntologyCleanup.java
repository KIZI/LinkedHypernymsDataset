package cz.vse.lhd.lhdontologycleanup;

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
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author tomas
 */
public class THDOntologyCleanup {

    private static final String dummyPredicate = "?"; //?
    private static final String typePredicate = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    private static final String subclassPredicate = "http://www.w3.org/2000/01/rdf-schema#subClassOf";
    private static final String equivPredicate = "http://www.w3.org/2002/07/owl#sameAs";
    private static final String seeAlsoPredicate = "http://www.w3.org/2000/01/rdf-schema#seeAlso";
    public static Integer maxLines = Integer.MAX_VALUE;
    /**
     * @param args the command line arguments
     */
    private static HashMap<String, ArrayList<String>> hyperHypo = new HashMap();
    private static HashMap<String, String> hypoHyper = new HashMap();
    private static BufferedWriter output_file_instances_notmapped_probably_existing_bw;
    private static BufferedWriter output_file_instances_mapped_existing_bw;
    private static BufferedWriter output_file_instances_notmapped_new_bw;
    private static BufferedWriter output_file_instances_notmapped_suspicious_bw;
    private static BufferedWriter output_file_instances_mapped_new_bw;
    private static BufferedWriter output_file_instances_mapped_new_notypebefore_bw;
    private static BufferedWriter output_file_classes_bw;
    private static BufferedWriter output_file_classes_equivallence_bw;
    private static BufferedWriter output_file_classes_subclass_bw;
    private static BufferedWriter output_file_classes_superclass_bw;
    private static BufferedWriter output_file_instances_less_sure_bw;
    private static BufferedWriter output_file_instances_notmapped_probably_existing_InEnglish_bw;
    private static BufferedWriter output_file_instances_mapped_existing_InEnglish_bw;
    private static BufferedWriter input_file_en_aligned_bw;
    private static BufferedWriter input_file_unique_bw;
    private static BufferedWriter output_input_file_typeoverride_bw;
    private static BufferedWriter input_file_instanceswithtypeonblacklistremoved_bw;
    private static ArrayList<String> allFilePathToGenerateStatsFrom = new ArrayList();
    // private static BufferedWriter output_file_instances_suspicious_bw;
    private static HashMap<String, String> mappingFromHypernymsToOWLclasses_exact = new HashMap();
    private static HashMap<String, String> mappingFromHypernymsToOWLclasses_approximate = new HashMap();

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

    private static String initUniqueFile(String resultPath, String lang) throws IOException {
        String uniqueFilePath = resultPath + lang + ".hypoutput.log.dbpedia.unique.nt";
        input_file_unique_bw = initOutputFile(uniqueFilePath, true);
        input_file_unique_bw.append("#THD Input file with duplicate lines removed \n");
        return uniqueFilePath;
    }

    private static String initTypeOverrideFile(String resultPath, String lang) throws IOException {
        String overridePath = resultPath + lang + ".hypoutput.log.dbpedia.typeoverride.nt";
        output_input_file_typeoverride_bw = initOutputFile(overridePath, true);
        output_input_file_typeoverride_bw.append("#Some types replaced according manually defined mapping \n");
        return overridePath;
    }

    private static String initAfterManualExclusionFile(String resultPath, String lang) throws IOException {
        String overridePath = resultPath + lang + ".hypoutput.log.dbpedia.manualexclusion.nt";
        input_file_instanceswithtypeonblacklistremoved_bw = initOutputFile(overridePath, true);
        input_file_instanceswithtypeonblacklistremoved_bw.append("#Instances with type on blacklist removed \n");
        return overridePath;
    }

    private static String initAlignedFile(String resultPath, String lang) throws IOException {
        String enalignedInputFilePath = resultPath + lang + "-en-aligned.nt";
        input_file_en_aligned_bw = initOutputFile(enalignedInputFilePath, true);
        input_file_en_aligned_bw.append("#THD Input file with objects replaced by their equivalents in English Wikipedia, if they exist\n");
        return enalignedInputFilePath;

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
        for (Entry<String, Integer> entry : sortedMap.entrySet()) {
            resWriter.append(entry.getValue() + "\t" + entry.getKey() + "\n");
        }
        System.gc();
        resWriter.close();

    }

    private static void initOutputFiles(String path, String lang) throws IOException {
        String basePath = path + lang;
        output_file_instances_notmapped_probably_existing_bw = initOutputFile(basePath + ".instances.notmapped.probablyexisting.nt", true);
        output_file_instances_notmapped_suspicious_bw = initOutputFile(basePath + ".instances.notmapped.suspicious.nt", true);
        output_file_instances_notmapped_suspicious_bw.append("#lists instances which have an instance as a type.\n");
        if (!lang.equals("en")) {
            output_file_instances_notmapped_probably_existing_InEnglish_bw = initOutputFile(basePath + ".instances.notmapped.probablyexisting.inEnglish.nt", true);
            output_file_instances_mapped_existing_InEnglish_bw = initOutputFile(basePath + ".instances.mapped.existing.inEnglish.nt", true);
            output_file_instances_notmapped_probably_existing_InEnglish_bw.append("#");
            output_file_instances_mapped_existing_InEnglish_bw.append("#Statement is not redundant w.r.t existing statements in the given language's DBpedia, but it was found as redundant against English DBpedia when the subject was mapped to English DBpedia using the published list of sameAs mappings, the object is mapped to the DBpedia ontology.\n");
        }
        output_file_instances_mapped_existing_bw = initOutputFile(basePath + ".instances.mapped.existing.nt", true);

        output_file_instances_notmapped_new_bw = initOutputFile(basePath + ".instances.notmapped.new.nt", true);
        output_file_instances_mapped_new_bw = initOutputFile(basePath + ".instances.mapped.new.nt", true);
        output_file_instances_mapped_new_notypebefore_bw = initOutputFile(basePath + ".instances.mapped.new.notypebefore.nt", true);

        output_file_classes_bw = initOutputFile(basePath + ".classes.nt", false);
        output_file_classes_equivallence_bw = initOutputFile(basePath + ".class.equivallence.nt", true);
        output_file_classes_superclass_bw = initOutputFile(basePath + ".class.superclass.nt", true);
        output_file_classes_subclass_bw = initOutputFile(basePath + ".class.subclasss.nt", true);
        output_file_instances_less_sure_bw = initOutputFile(basePath + ".instances.less_sure.nt", true);


        output_file_instances_notmapped_probably_existing_bw.append("#Statements where the name of the object does not match any of the DBpedia-owl types, but it exactly matches a name of another assigned type in DBpedia (such as from the schema.org) ontology. These statements are thus redundant.\n");
        //but this type matches the name of the wikipedia resource
        // example: http://dbpedia.org/resource/Garden_Networks have thd assigned type http://en.wikipedia.org/wiki/Organization
        // dbpedia: Organization does not match dbpedia-owl:Organisation ==> mappedHypernym= null
        // but Organization matches existing dbpedia type http://schema.org/Organization

        output_file_instances_mapped_existing_bw.append("#Statements with object mapped to the DBpedia Ontology, the statements are redundant w.r.t. existing statement in DBpedia\n");
        output_file_instances_notmapped_new_bw.append("#Statements with the object not mapped to the DBpedia Ontology, the statements uniqueness w.r.t existing statements in DBpedia was not checked\n");
        output_file_instances_mapped_new_bw.append("#Statements with the object mapped to the DBpedia Ontology, the statements are not redundant w.r.t existing statements in DBpedia\n");
        output_file_instances_mapped_new_notypebefore_bw.append("#Statements with the object mapped to the DBpedia Ontology, the statements are not redundant, since the subjects do not have any type in DBpedia\n");
        output_file_classes_equivallence_bw.append("#Sameas mappings from DBpedia article to DBpedia ontology\n");
        output_file_classes_superclass_bw.append("#The mappings from DBpedia article to its superclass - a DBpedia ontology class\n#These are most likely all wrong, since the confirmed mappings from *.class.superclass.confirmed.nt do not appear in this file \n");
        output_file_classes_subclass_bw.append("#The mappings from DBpedia article to its subclass - a DBpedia ontology class\n");
        output_file_instances_less_sure_bw.append("#Statements with the subject used as object (i.e. hypernym) in another extracted statement and at the same time the object is considered as instance in DBpedia\n");



        //output_file_instances_suspicious_bw = new BufferedWriter(output_file_instances_suspicious_fw);        
    }

    /* private static BufferedWriter getWriter(Boolean isNew, Boolean isMapped, Boolean noTypeBefore, Boolean includesCheckAgainstEnglishDBpedia)
     {

     if (isNew)
     {
     if (isMapped)
     {
     if (noTypeBefore)
     {
     return output_file_instances_mapped_new_notypebefore_bw;
     }
     else
     {
     return output_file_instances_mapped_new_bw;
     }
     }
     else
     {
     return output_file_instances_notmapped_new_bw;
     }
     }
     else
     {
     if (isMapped)
     {
     if (includesCheckAgainstEnglishDBpedia)
     {
     return output_file_instances_mapped_existing_InEnglish_bw;
     }
     else
     {
     return output_file_instances_mapped_existing_bw;
     }                
     }
     else
     {
     if (includesCheckAgainstEnglishDBpedia)
     {
     return output_file_instances_notmapped_probably_existing_InEnglish_bw;
     }
     else
     {
     return output_file_instances_notmapped_probably_existing_bw;
     }
     }
     }
     }*/
    /*
     * input <hypo> <?> <hyper>, 
     * if exists <hyper2> such as <hyper> <?> <hyper2> then <?> is replaced by <rdfs:subClassOf>
     * otherwise <?> is replaced by <rdf:type>
     */
    public static String getConceptName(String uri) {
        return uri.substring(uri.lastIndexOf("/") + 1);
    }
    /*
     * the ENfallbackLangInstanceClassMappings is null if the processed language is English
     */

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

    private static void detectEntityType(InstanceCheck instanceClassMappings, InstanceCheck fallbackClassMappings, LanguageMapping lm) throws IOException {
        //TODO check if suspicious, if yes store to different file
        for (String hypo : hypoHyper.keySet()) {
            String hypoName = getConceptName(hypo);
            String hyper = hypoHyper.get(hypo);
            String hyperName = getConceptName(hypoHyper.get(hypo));
            String hypoName_enmapped = null;
            if (fallbackClassMappings != null) {
                hypoName_enmapped = lm.getEnglishName(hypoName);
                /*if (hypoName_enmapped!=null)
                 {
                 System.out.println(hypoName+"-->"+hypoName_enmapped);
                 }*/
            }
            //either english or given language
            boolean hypoIsMarkedAsInstanceInDBpedia;
            hypoIsMarkedAsInstanceInDBpedia = instanceClassMappings.isInstance(hypoName);
            boolean hyperIsMarkedAsInstanceInDBpedia = instanceClassMappings.isInstance(hyperName);
            //fallback is the english dbpedia file if the processed language is other than english dbpedia
            //TODO add this fallback also for hyperIsMarkedAsInstanceInDBpedia (?)
            if (hypoIsMarkedAsInstanceInDBpedia == false && fallbackClassMappings != null) {
                hypoIsMarkedAsInstanceInDBpedia = fallbackClassMappings.isInstance(hypoName_enmapped);
            }

            //hypernymCheck is currently done only against the given language results
            boolean hypoIsHypernym = hyperHypo.containsKey(hypo);

            String mappedHypernym = mappingFromHypernymsToOWLclasses_exact.get(hyper);
            MappingMethodEnum mappingMethod;
            if (mappedHypernym != null) {
                mappingMethod = MappingMethodEnum.exact;
                hyper = mappedHypernym;
            } else {
                mappedHypernym = mappingFromHypernymsToOWLclasses_approximate.get(hyper);

                if (mappedHypernym != null) {
                    mappingMethod = MappingMethodEnum.subclass;
                    //to the output file the original hypernym is saved, as saving the mappedhypernyn would loose information
                    //the mapped hypernym is a more generic concept than the original hypernym.
                    // the link from the hypernym and the mapped hypernym can be inferred from the class.superclass.nt file
                    //hyper = mappedHypernym;
                } else {
                    mappingMethod = MappingMethodEnum.notmapped;
                }
            }




            if (hypoIsHypernym) {
                if (hypoIsMarkedAsInstanceInDBpedia) {
                    // this indicates, that as class the hypo is suspicious,
                    //empirically, the DBpedia is mostly right - the hypo is indeed an instance and thetype predicate should be used
                    saveTriple(hypo, typePredicate, hypoHyper.get(hypo), output_file_instances_less_sure_bw);
                    //output_file_instances_less_sure_bw.append("<" + hypo + "> <" + typePredicate + "> <" + hypoHyper.get(hypo) + "> .\n");   
                } else {
                    saveTriple(hypo, subclassPredicate, hypoHyper.get(hypo), output_file_classes_bw);
                    //output_file_classes_bw.append("<" + hypo + "> <" + subclassPredicate + "> <" + hypoHyper.get(hypo) + "> .\n");                
                }
            } else {
                //the hypernym is for sure new as the hypo does not have any types in DBpedia so far.
                if (!hypoIsMarkedAsInstanceInDBpedia) {
                    //is not instance in given language DBpedia nor English DBpedia /if given language is different from English/

                    //in DBpedia many articles lack any type, i.e. they are not considered as instances
                    // missing type for an instance means, that the THD returned type is for sure new (rather than suspicious)
                    if (mappingMethod != MappingMethodEnum.notmapped) {
                        saveTriple(hypo, typePredicate, hyper, output_file_instances_mapped_new_notypebefore_bw);
                        //output_file_instances_mapped_new_notypebefore_bw.append("<" + hypo + "> <" + typePredicate + "> <" + hyper + "> .\n");
                    } else {
                        if (!hyperIsMarkedAsInstanceInDBpedia) {
                            saveTriple(hypo, typePredicate, hyper, output_file_instances_notmapped_new_bw);
                            //output_file_instances_notmapped_new_bw.append("<" + hypo + "> <" + typePredicate + "> <" + hyper + "> .\n");
                        } else {
                            saveTriple(hypo, typePredicate, hyper, output_file_instances_notmapped_suspicious_bw);
                            //output_file_instances_notmapped_suspicious_bw.append("<" + hypo + "> <" + seeAlsoPredicate + "> <" + hyper + "> .\n");
                        }

                    }
                } //the hypo has some types, it is necessary to find out whether one of them matches the hyper
                else {
                    //first check if the DBpedia instance file for the given language does not contain the entry
                    if (instanceClassMappings.isInstanceType(hypoName, getConceptName(hyper))) {
                        //if mappedHypernym is null, it means that the instance has a type from another ontology than the dbpedia ontology, 
                        //but this type matches the name of the wikipedia resource
                        // example: http://dbpedia.org/resource/Garden_Networks have thd assigned type http://en.wikipedia.org/wiki/Organization
                        // dbpedia: Organization does not match dbpedia-owl:Organisation ==> mappedHypernym= null
                        // but Organization matches existing dbpedia type http://schema.org/Organization
                        if (mappingMethod != MappingMethodEnum.notmapped) {
                            //since the hypernym was mapped to DBpedia and its name matches an object of an existing statement,
                            //it is (almost) certain that this object is also a DBpedia class
                            saveTriple(hypo, typePredicate, hyper, output_file_instances_mapped_existing_bw);
                            //output_file_instances_mapped_existing_bw.append("<" + hypo + "> <" + typePredicate + "> <" + hyper + "> .\n");
                        } else {
                            //since the hypernym was NOT mapped to DBpedia and its name matches an object of an existing statement,
                            //it is certain that the object of the matching statement comes from other than DBpedia ontology                            
                            saveTriple(hypo, typePredicate, hyper, output_file_instances_notmapped_probably_existing_bw);
                            //output_file_instances_notmapped_probably_existing_bw.append("<" + hypo + "> <" + typePredicate + "> <" + hyper + "> .\n");
                        }
                    } //second, check if the DBpedia instance file for English does not contain the entry
                    // to perform this check, we need to have the hypo mapped to English (hypoName_enmapped!=null)
                    // this check is not performed if the processed language is English
                    else if (hypoName_enmapped != null && fallbackClassMappings.isInstanceType(hypoName_enmapped, getConceptName(hyper))) {
                        if (mappingMethod != MappingMethodEnum.notmapped) {
                            //since the hypernym was mapped to DBpedia and its name matches an object of an existing statement in English DBpedia,
                            //it is (almost) certain that this object is also a DBpedia class
                            saveTriple(hypo, typePredicate, hyper, output_file_instances_mapped_existing_InEnglish_bw);
                            //output_file_instances_mapped_existing_InEnglish_bw.append("<" + hypo + "> <" + typePredicate + "> <" + hyper + "> .\n");
                        } else {
                            //since the hypernym was NOT mapped to DBpedia and its name matches an object of an existing statement,
                            //it is certain that the object of the matching statement comes from other than DBpedia ontology                            
                            saveTriple(hypo, typePredicate, hyper, output_file_instances_notmapped_probably_existing_InEnglish_bw);
                            //output_file_instances_notmapped_probably_existing_InEnglish_bw.append("<" + hypo + "> <" + typePredicate + "> <" + hyper + "> .\n");
                        }
                    } //the statement does not match any existing statement, it is new
                    else {
                        if (mappingMethod != MappingMethodEnum.notmapped) {
                            saveTriple(hypo, typePredicate, hyper, output_file_instances_mapped_new_bw);
                            //output_file_instances_mapped_new_bw.append("<" + hypo + "> <" + typePredicate + "> <" + hyper + "> .\n");
                        } else {
                            saveTriple(hypo, typePredicate, hyper, output_file_instances_notmapped_new_bw);
                            //output_file_instances_notmapped_new_bw.append("<" + hypo + "> <" + typePredicate + "> <" + hyper + "> .\n");
                        }

                    }
                    //TODO : even in this branch here some types may be new - it is necessary to check whether the types overlap

                }
            }
        }
        output_file_classes_bw.close();
        output_file_instances_notmapped_probably_existing_bw.close();
        output_file_instances_notmapped_new_bw.close();
        output_file_instances_mapped_existing_bw.close();
        output_file_instances_mapped_new_bw.close();
        output_file_instances_mapped_new_notypebefore_bw.close();
        output_file_instances_less_sure_bw.close();
        output_file_instances_notmapped_suspicious_bw.close();
        if (output_file_instances_notmapped_probably_existing_InEnglish_bw != null) {
            output_file_instances_notmapped_probably_existing_InEnglish_bw.close();
        }
        if (output_file_instances_mapped_existing_InEnglish_bw != null) {
            output_file_instances_mapped_existing_InEnglish_bw.close();
        }

    }

    public static void main(String[] args) throws Exception {
        // TODO code application logic here
        String basePath = Conf.outputDir();
        String lang = Conf.lang();
        if (args.length != 2) {
            System.out.println("argument 1 'Base path' not provided, using default=" + basePath);
            System.out.println("argument 2 'language' not provided, using default=" + lang);
            /*System.out.println("Running in the desktop mode (limiting the number of processed lines to avoid out of memory)");
            maxLines = 1000000;*/
        } else {
            basePath = args[0];
            lang = args[1];
        }
        if (!basePath.endsWith("/")) {
            basePath = basePath + "/";
        }
        String ontologyPath = Conf.datasetOntologyPath();
        System.out.println("ontologyPath=" + ontologyPath);

        String thdInputPath = basePath + "hypoutput.log.dbpedia";
        System.out.println("thdInputPath=" + thdInputPath);

        String thdInputOnlyUniqueLines = removeDuplicateLinesInInstanceFile(thdInputPath, basePath, lang);
        System.out.println("thdInputOnlyUniqueLines=" + thdInputOnlyUniqueLines);
        String enAlignedInputPath;
        String DBpediaInstancesEnglishFilePath = Conf.datasetInstance_typesEnPath();
        System.out.println("DBpediaInstancesEnglishFilePath=" + DBpediaInstancesEnglishFilePath);
        String DBpediaInstancesFilePath = Conf.datasetInstance_typesPath();
        System.out.println("DBpediaInstancesFilePath=" + DBpediaInstancesFilePath);
        String DBpediaInstancesExceptions = basePath + "ExceptionsFromInstancesFileByType";
        //if a DBpedia entity has in instance file a type from this list, it is a class not an instance
        System.out.println("DBpediaInstancesExceptions=" + DBpediaInstancesExceptions);
        String overrideTypesPath = basePath + "override-types_" + lang;
        System.out.println("OverrideTypesPath=" + overrideTypesPath);
        String excludeTypes = basePath + "exclude-types";

        ManualMapping mm = new ManualMapping(overrideTypesPath, excludeTypes);
        String typesOverridenPath = manualTypeOverride(thdInputOnlyUniqueLines, basePath, mm, lang);


        System.out.println("ExcludeTypesPath=" + excludeTypes);

        String supplementalOntologyPath_EN = basePath + "en.class.superclass.confirmed.nt";

        System.out.println("supplementalOntologyPath=" + supplementalOntologyPath_EN);

        initOutputFiles(basePath, lang);
        InstanceCheck firstLangClassMappings = null;
        LanguageMapping mapping = null;
        InstanceCheck ENfallbackLangInstanceClassMappings;


        if (!lang.equals("en")) {
            String mappingFile = Conf.datasetInterlanguage_linksPath();//.excerpt
            System.out.println("mappingFile=" + mappingFile);
            mapping = new LanguageMapping(mappingFile, lang);
            enAlignedInputPath = translateTypesInInstanceFile(typesOverridenPath, basePath, mapping, lang);
            System.out.println("enAlignedInputPath=" + enAlignedInputPath);
            System.out.println("The result of mapping to English DBpedia saved to enAlignedInputPath=" + enAlignedInputPath);
            //release memory
            //mapping = null;
            //System.gc();
            firstLangClassMappings = new InstanceCheck(DBpediaInstancesFilePath, DBpediaInstancesExceptions);
            ENfallbackLangInstanceClassMappings = new InstanceCheck(DBpediaInstancesEnglishFilePath, DBpediaInstancesExceptions);
        } else {
            firstLangClassMappings = new InstanceCheck(DBpediaInstancesEnglishFilePath, DBpediaInstancesExceptions);
            ENfallbackLangInstanceClassMappings = null;
            enAlignedInputPath = typesOverridenPath;
        }
        String finalInstancesPath = deleteInstancesWithTypeOnExclusionList(enAlignedInputPath, basePath, mm, lang);

        buildHyperHypoHashMap(finalInstancesPath);


        OntologyMapping omEnglish = new OntologyMapping(ontologyPath, supplementalOntologyPath_EN, "en");
        OntologyMapping omLanguageDependent = null;
        if (!lang.equals("en")) {
            String supplementalOntologyPath_lang_dep = basePath + lang + ".class.superclass.confirmed.nt";

            System.out.println("supplementalOntologyPath_Language_dep=" + supplementalOntologyPath_lang_dep);

            // this contains mappings from non-English alternative names to dbpedia-owl classes
            //and is used in generate* functions when the statement's object failed to map to English DBpedia in translateTypesInInstanceFile

            //for dutch this is completely useless, since there are no alternative names for nl in dbpedia_3.8.owl, 
            // but there are many for de
            omLanguageDependent = new OntologyMapping(ontologyPath, supplementalOntologyPath_lang_dep, lang);
        }
        generateSameAsForTypes(omEnglish, omLanguageDependent);
        generateSubclassForTypes(omEnglish, omLanguageDependent);
        generateSuperClassForTypes(omEnglish, omLanguageDependent);

        detectEntityType(firstLangClassMappings, ENfallbackLangInstanceClassMappings, mapping);
        for (String path : allFilePathToGenerateStatsFrom) {
            generateFileWithListOfTypesAndFrequency(path);
        }



        //listSuspiciousEntries();
        //REMOVE LINES from INSTANCES WHICH CONTAIN AS HYPERNYM A CONCEPT, WHICH DOES NOT HAVE ANY TYPE IN DBPEDIA INSTANCES FILE
        //i.e. the type is not present is subject in dbpedia_3.8.owl   
    }

    public static String removeDuplicateLinesInInstanceFile(String input, String resultPath, String lang) throws FileNotFoundException, IOException {
        String resultFilePath = initUniqueFile(resultPath, lang);
        HashSet all = new HashSet();

        FileInputStream fstream = new FileInputStream(input);
        // Get the object of DataInputStream
        DataInputStream in = new DataInputStream(fstream);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String thisLine;
        String lineEnd = "";
        while ((thisLine = br.readLine()) != null) {
            if (all.contains(thisLine)) {
                continue;
            } else {
                input_file_unique_bw.append(lineEnd + thisLine);
                lineEnd = "\n";
                all.add(thisLine);
            }

        }
        System.gc();
        input_file_unique_bw.close();
        return resultFilePath;
    }

    private static String translateTypesInInstanceFile(String origTHDinputFile, String resultsPath, LanguageMapping mapping, String lang) throws FileNotFoundException, IOException {

        String alignedFilePath = initAlignedFile(resultsPath, lang);
        FileInputStream fstream = new FileInputStream(origTHDinputFile);
        // Get the object of DataInputStream
        DataInputStream in = new DataInputStream(fstream);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String thisLine;
        int mappedCount = 0;
        int notMappedCount = 0;
        while ((thisLine = br.readLine()) != null) {
            if (thisLine.startsWith("#")) {
                continue;
            }

            int indexOfSubjectStart = thisLine.indexOf("<");
            int indexOfSubjectEnd = thisLine.indexOf(">");

            String subject = thisLine.substring(indexOfSubjectStart + 1, indexOfSubjectEnd);

            int indexOfObjectEnd = thisLine.lastIndexOf(">");
            int indexOfObjectStart = thisLine.lastIndexOf("/");
            String objectName = thisLine.substring(indexOfObjectStart + 1, indexOfObjectEnd);
            String objectMapped = mapping.getEnglishName(objectName);
            if (objectMapped == null) {
                objectMapped = objectName;
                input_file_en_aligned_bw.append("<" + subject + "> <" + dummyPredicate + "> <http://" + lang + ".dbpedia.org/resource/" + objectName + "> .\n");
                notMappedCount++;
            } else {
                mappedCount++;
                input_file_en_aligned_bw.append("<" + subject + "> <" + dummyPredicate + "> <http://dbpedia.org/resource/" + objectMapped + "> .\n");
            }
        }
        input_file_en_aligned_bw.append("#DBpedia  mapped=" + mappedCount + " not mapped " + notMappedCount);
        input_file_en_aligned_bw.close();
        return alignedFilePath;
    }

    private static void generateSameAsForTypes(OntologyMapping omEnglish, OntologyMapping omLanguageDep) throws IOException {
        for (String name : hyperHypo.keySet()) {
            String result;
            if (name.contains("http://dbpedia.org")) {
                result = omEnglish.map_sameAs(name);
            } //the mapping to English DBpedia in translateTypesInInstanceFile failed
            //TODO: activate this if result == null
            else if (name.contains("dbpedia.org")) {
                result = omLanguageDep.map_sameAs(name);
            } else {
                result = null;
                System.out.println("Incorrect input type:" + name);
            }

            if (result != null) {
                mappingFromHypernymsToOWLclasses_exact.put(name, result);
                //this test is necessary, becasue the manually defined classes are also from the dbpedia namespace
                //e.g. http://dbpedia.org/resource/Display may be found to match http://dbpedia.org/resource/Display in the manually defined file
                if (!name.equals(result)) {
                    output_file_classes_equivallence_bw.append("<" + name + "> <" + equivPredicate + "> <" + result + "> .\n");
                } else {
                }

            }

        }
        output_file_classes_equivallence_bw.close();
    }      /*
     * call after generateSameAsForTypes
     * finds subclasses for THD-generated type - not really useful at the moment
     */


    private static void generateSubclassForTypes(OntologyMapping omEnglish, OntologyMapping omLanguageDep) throws IOException {
        for (String name : hyperHypo.keySet()) {
            String result;
            if (name.contains("http://dbpedia.org")) {
                result = omEnglish.map_subclass(name);
            } else if (name.contains("dbpedia.org")) {
                result = omLanguageDep.map_subclass(name);
            } else {
                result = null;
                System.out.println("Incorrect input type:" + name);
            }

            if (result != null) {
                output_file_classes_subclass_bw.append("<" + result + "> <" + subclassPredicate + "> <" + name + "> .\n");
            }

        }
        output_file_classes_subclass_bw.close();
    }
    /*
     * call after generateSameAsForTypes
     * finds superclass for THD-generated type
     * the superclass is added to mappingFromHypernymsToOWLclasses map if an equivallent class for the THD-generated type does not exist
     */

    private static void generateSuperClassForTypes(OntologyMapping omEnglish, OntologyMapping omLanguageDep) throws IOException {
        if (mappingFromHypernymsToOWLclasses_exact.isEmpty()) {
            System.err.println("Have you called generateSameAsForTypes first?");
        }
        for (String name : hyperHypo.keySet()) {

            String result;

            if (name.contains("http://dbpedia.org")) {
                result = omEnglish.map_superclass(name);
            } else if (name.contains("dbpedia.org")) {
                result = omLanguageDep.map_superclass(name);
            } else {
                result = null;
                System.out.println("Incorrect input type:" + name);
            }
            if (result != null) {
                /*
                 * the current hypernyms is mapped to the superclass only if 
                 * equivallent class was not previously found previously in generateSameAsForTypes
                 * 
                 */
                if (!mappingFromHypernymsToOWLclasses_exact.containsKey(name)) {
                    if (mappingFromHypernymsToOWLclasses_approximate.containsKey(name)) {
                        System.out.println("Hypernym '" + name + "' was previously mapped to '" + mappingFromHypernymsToOWLclasses_approximate.get(name)
                                + "' the new mapping to '" + result + "' is discarded");
                    } else {
                        output_file_classes_superclass_bw.append("<" + name + "> <" + subclassPredicate + "> <" + result + "> .\n");
                        mappingFromHypernymsToOWLclasses_approximate.put(name, result);
                    }

                }


            }

        }
        output_file_classes_superclass_bw.close();
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


                String hypo = subject;
                String hyper = object;
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

    private static String manualTypeOverride(String inputFile, String resultsPath, ManualMapping mm, String lang) throws FileNotFoundException, IOException {

        String overridenPath = initTypeOverrideFile(resultsPath, lang);
        FileInputStream fstream = new FileInputStream(inputFile);
        // Get the object of DataInputStream
        DataInputStream in = new DataInputStream(fstream);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String thisLine;
        int mappedCount = 0;
        int notMappedCount = 0;
        while ((thisLine = br.readLine()) != null) {
            if (thisLine.startsWith("#")) {
                continue;
            }

            int indexOfSubjectStart = thisLine.indexOf("<");
            int indexOfSubjectEnd = thisLine.indexOf(">");

            String subject = thisLine.substring(indexOfSubjectStart + 1, indexOfSubjectEnd);

            int indexOfObjectEnd = thisLine.lastIndexOf(">");
            int indexOfObjectStart = thisLine.lastIndexOf("<");
            String object = thisLine.substring(indexOfObjectStart + 1, indexOfObjectEnd);
            String objectOverriden = mm.getOverridingType(object);

            if (objectOverriden == null) {
                objectOverriden = object;
                output_input_file_typeoverride_bw.append("<" + subject + "> <" + dummyPredicate + "> <" + object + "> .\n");
                notMappedCount++;
            } else {
                mappedCount++;
                output_input_file_typeoverride_bw.append("<" + subject + "> <" + dummyPredicate + "> <" + objectOverriden + "> .\n");
            }
        }
        output_input_file_typeoverride_bw.append("#DBpedia  types overriden =" + mappedCount + " not overriden " + notMappedCount);
        output_input_file_typeoverride_bw.close();
        return overridenPath;
    }

    private static String deleteInstancesWithTypeOnExclusionList(String inputFile, String resultsPath, ManualMapping mm, String lang) throws FileNotFoundException, IOException {

        String afterExclusionPath = initAfterManualExclusionFile(resultsPath, lang);
        FileInputStream fstream = new FileInputStream(inputFile);
        // Get the object of DataInputStream
        DataInputStream in = new DataInputStream(fstream);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String thisLine;
        int excludedCount = 0;
        int notExcludedCount = 0;
        while ((thisLine = br.readLine()) != null) {
            if (thisLine.startsWith("#")) {
                continue;
            }

            int indexOfSubjectStart = thisLine.indexOf("<");
            int indexOfSubjectEnd = thisLine.indexOf(">");

            String subject = thisLine.substring(indexOfSubjectStart + 1, indexOfSubjectEnd);

            int indexOfObjectEnd = thisLine.lastIndexOf(">");
            int indexOfObjectStart = thisLine.lastIndexOf("<");
            String object = thisLine.substring(indexOfObjectStart + 1, indexOfObjectEnd);
            boolean isExluded = mm.isExcluded(object);

            if (isExluded == false) {
                input_file_instanceswithtypeonblacklistremoved_bw.append("<" + subject + "> <" + dummyPredicate + "> <" + object + "> .\n");
                notExcludedCount++;
            } else {
                excludedCount++;
            }
        }
        input_file_instanceswithtypeonblacklistremoved_bw.append("#DBpedia  instances excluded =" + excludedCount + " not excluded " + notExcludedCount);
        input_file_instanceswithtypeonblacklistremoved_bw.close();
        return afterExclusionPath;
    }
}
