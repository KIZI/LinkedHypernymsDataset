package cz.vse.lhd.hypernymextractor.builder;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Corpus;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.Node;
import gate.ProcessingResource;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.creole.SerialAnalyserController;
import gate.util.GateException;
import gate.util.InvalidOffsetException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Milan Dojchinovski
 * <milan (at) dojchinovski (dot) mk>
 * Twitter:
 * @m1ci www: http://dojchinovski.mk
 */
public class HypernymExtractor {

    private static HypernymExtractor hypernymExtractor = null;
    private SerialAnalyserController pipeline = null;
    private SerialAnalyserController resetPipeline = null;
    private static String hypernymLoggingPath;
    boolean prepared = false;
    private static boolean saveInTriplets;
    AnnotationSet as_all;
    AnnotationSet as_hearst;
    String hyp_text;
    long hyp_start;
    long hyp_end;
    private static String lang = "en";
    private static boolean isInitialized = false;
    public static String JAPEPATH;
    private static String taggerBinary_DE;
    private static String taggerBinary_NL;

    public static void init(String _lang, String _JAPEPATH, String _hypernymLoggingPath, String _taggerBinary_DE, String _taggerBinary_NL, boolean _saveInTriplets) {
        lang = _lang;
        JAPEPATH = _JAPEPATH;
        isInitialized = true;
        saveInTriplets = _saveInTriplets;
        hypernymLoggingPath = _hypernymLoggingPath;
        taggerBinary_DE = _taggerBinary_DE;
        taggerBinary_NL = _taggerBinary_NL;
    }

    public static HypernymExtractor getInstance() throws GateException, MalformedURLException {
        if (!isInitialized) {
            Logger.getGlobal().log(Level.SEVERE, null, "Run init first");
            return null;
        }
        if (hypernymExtractor == null) {

            Gate.getCreoleRegister().registerDirectories(
                    new File(Gate.getPluginsHome(), "Tagger_Framework").toURL());



            hypernymExtractor = new HypernymExtractor();
            return hypernymExtractor;
        } else {
            return hypernymExtractor;
        }
    }

    public void extractHypernyms(Corpus corpus) {
        if (!prepared) {
            preparePipeline();
        }

        try {
            pipeline.setCorpus(corpus);
            pipeline.execute();
            for (Document doc : corpus) {
                as_all = doc.getAnnotations();


                as_hearst = as_all.get("h");
                Iterator ann_iter = as_hearst.iterator();
                if (!ann_iter.hasNext()) {
                    String articleName = (String) doc.getFeatures().get("article_title");
                    Logger.getGlobal().log(Level.WARNING, "Article ''{0}'' no annotation ''h'' found.", articleName);
                    //MyLogger.log(articleName + " ;NO Hypernym found");
                }

                while (ann_iter.hasNext()) {
                    Annotation isaAnnot = (gate.Annotation) ann_iter.next();
                    Node isaStart = isaAnnot.getStartNode();
                    Node isaEnd = isaAnnot.getEndNode();
                    String hypernym = doc.getContent().getContent(isaStart.getOffset(), isaEnd.getOffset()).toString();

                    // override with lemma feature if present
                    if (isaAnnot.getFeatures().containsKey("lemma")) {
                        String hypCand = isaAnnot.getFeatures().get("lemma").toString();
                        if (hypCand != null && hypCand.length() > 0 && !hypCand.equals("<unknown>")) {
                            hypernym = hypCand;
                        }
                        if (!hypernym.equals(hypCand)) {
                            Logger.getGlobal().log(Level.INFO, "Replacing hypernym in text {0} with its DIFFERENT lemma: {1}", new Object[]{hypernym, hypCand});
                        }
                    }

                    Logger.getGlobal().log(Level.INFO, "HYPERNYM: {0}", hypernym);

                    if (hypernymLoggingPath != null && !"".equals(hypernymLoggingPath)) {
                        String dbPediaURL;
                        if (saveInTriplets) {
                            String urlName = (String) doc.getFeatures().get("dbpedia_url");
                            String urlHypernym = DBpediaLinker.getInstance().getLink(hypernym);
                            if (urlHypernym == null) {
                                Logger.getGlobal().log(Level.WARNING, "Hypernym found and not mapped: {0}", hypernym);
                            } else {
                                saveHypernym(urlName, urlHypernym, true);
                                doc.getFeatures().put("thdDBpediaType_search", urlName);
                            }
                        }
                        saveHypernym((String) doc.getFeatures().get("article_title"), hypernym, false);
                    } else {
                        //MyLogger.log((String) doc.getFeatures().get("article_title") + " ;Hypernym found");
                    }



                    //resetPipeline.setCorpus(corpus);
                    //resetPipeline.execute();
                    FeatureMap f = Factory.newFeatureMap();
                    //f.put("url", Linker.getInstance().getWikiLinkEN(hypernym,"local"));  
                    doc.getAnnotations().add(isaStart, isaEnd, "a", f);
                }
            }

        } catch (InvalidOffsetException ex) {
            Logger.getGlobal().log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getGlobal().log(Level.SEVERE, null, ex);
        }
    }

    private void saveHypernym(String source, String hypernym, boolean mapToDBpedia) {

        try {

            StringBuilder content = new StringBuilder();
            if (mapToDBpedia == false) {
                content.append(source).append(";").append(hypernym).append("\n");
            } else {
                content.append("<");
                content.append(source);
                content.append(">");
                //content.append(" <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ");
                content.append(" <?> ");
                content.append("<");
                content.append(hypernym);
                content.append(">.\n");
            }

            String concreteHypernymLoggingPath = hypernymLoggingPath;
            if (mapToDBpedia) {
                concreteHypernymLoggingPath = concreteHypernymLoggingPath + ".dbpedia";
            } else {
                concreteHypernymLoggingPath = concreteHypernymLoggingPath + ".raw";
            }
            File file = new File(concreteHypernymLoggingPath);

            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content.toString());
            bw.close();

        } catch (IOException e) {
            //e.printStackTrace();
        }

    }

    private void preparePipeline() {
        try {
            prepared = true;

            //pipeline = (SerialAnalyserController) Factory.createResource("gate.creole.SerialAnalyserController");
            pipeline = (SerialAnalyserController) Factory.createResource("gate.creole.RealtimeCorpusController");
            FeatureMap resetFeatureMap = Factory.newFeatureMap();
            ProcessingResource resetPR = (ProcessingResource) Factory.createResource("gate.creole.annotdelete.AnnotationDeletePR", resetFeatureMap);
            pipeline.add(resetPR);

            FeatureMap tokenizerFeatureMap = Factory.newFeatureMap();
            ProcessingResource tokenizerPR = (ProcessingResource) Factory.createResource("gate.creole.tokeniser.DefaultTokeniser", tokenizerFeatureMap);

            pipeline.add(tokenizerPR);

            if (lang.equals("en")) {
                FeatureMap sentenceSplitterFeatureMap = Factory.newFeatureMap();
                ProcessingResource sentenceSplitterPR = (ProcessingResource) Factory.createResource("gate.creole.splitter.RegexSentenceSplitter", sentenceSplitterFeatureMap);

                FeatureMap posTaggerFeatureMap = Factory.newFeatureMap();
                ProcessingResource posTaggerPR = (ProcessingResource) Factory.createResource("gate.creole.POSTagger", posTaggerFeatureMap);
                pipeline.add(sentenceSplitterPR);
                pipeline.add(posTaggerPR);
            } else if (lang.equals("nl") | lang.equals("de")) {





                FeatureMap taggerFeatureMap = Factory.newFeatureMap();
                taggerFeatureMap.put("debug", "false");

                if (!lang.equals("de")) {
                    taggerFeatureMap.put("encoding", "utf-8");
                } else {
                    // for de, utf-8 causes on some documents tagger to fail
                    taggerFeatureMap.put("encoding", "ISO-8859-1");
                }

                taggerFeatureMap.put("failOnUnmappableCharacter", "false");
                taggerFeatureMap.put("featureMapping", "lemma=3;category=2;string=1");
                taggerFeatureMap.put("inputAnnotationType", "Token");
                taggerFeatureMap.put("inputTemplate", "${string}");
                taggerFeatureMap.put("outputAnnotationType", "Token");
                taggerFeatureMap.put("regex", "(.+)	(.+)	(.+)");
                if (lang.equals("de")) {
                    taggerFeatureMap.put("taggerBinary", taggerBinary_DE);

                } else if (lang.equals("nl")) {
                    taggerFeatureMap.put("taggerBinary", taggerBinary_NL);
                }
                /*taggerFeatureMap.put("taggerFlags", "[]");
                 taggerFeatureMap.put("updateAnnotations", "true");
                            
                 taggerFeatureMap.put("preProcessURL", "");
                 taggerFeatureMap.put("postProcessURL", "");
                            
                 taggerFeatureMap.put("inputASName", "");
                 taggerFeatureMap.put("outputASName", "");
                 taggerFeatureMap.put("taggerDir", "");*/

                ProcessingResource genTag = (ProcessingResource) Factory.createResource("gate.taggerframework.GenericTagger", taggerFeatureMap);
                //genTag.setFeatures(taggerFeatureMap); 



                pipeline.add(genTag);
            }


            File japeOrigFile = new File(JAPEPATH);
            //File japeOrigFile = new File("/Users/Milan/Documents/Programming/repositories/linkedtv/WP2/THD/code/CorpusBuilderPR/en_hearst.jape");
            java.net.URI japeURI = japeOrigFile.toURI();
            FeatureMap transducerFeatureMap = Factory.newFeatureMap();
            try {
                transducerFeatureMap.put("grammarURL", japeURI.toURL());
                transducerFeatureMap.put("encoding", "UTF-8");
            } catch (MalformedURLException e) {
                System.out.println("Malformed URL of JAPE grammar");
                System.out.println(e.toString());
            }
            ProcessingResource japeCandidatesPR = (ProcessingResource) Factory.createResource("gate.creole.Transducer", transducerFeatureMap);

            pipeline.add(japeCandidatesPR);

            resetPipeline = (SerialAnalyserController) Factory.createResource("gate.creole.SerialAnalyserController");

            FeatureMap resetFeatureMap2 = Factory.newFeatureMap();
            ProcessingResource resetPR2 = (ProcessingResource) Factory.createResource("gate.creole.annotdelete.AnnotationDeletePR", resetFeatureMap2);

            resetPipeline.add(resetPR2);

        } catch (ResourceInstantiationException ex) {
            Logger.getGlobal().log(Level.SEVERE, null, ex);
        }
    }
}
