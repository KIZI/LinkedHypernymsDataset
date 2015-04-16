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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringEscapeUtils;

/**
 * @author Milan Dojchinovski
 * <milan (at) dojchinovski (dot) mk>
 * Twitter:
 * @m1ci www: http://dojchinovski.mk
 */
public class HypernymExtractor {

    private ProcessStatus loader;
    private SerialAnalyserController pipeline = null;
    private SerialAnalyserController resetPipeline = null;
    private final String hypernymLoggingPath;
    boolean prepared = false;
    private final boolean saveInTriplets;
    private String lang = "en";
    public String JAPEPATH;
    private final String taggerBinary_DE;
    private final String taggerBinary_NL;
    private final PrintWriter dbpediaOutputFile;
    private final PrintWriter rawOutputFile;
    private final DBpediaLinker dbpediaLinker;

    public HypernymExtractor(DBpediaLinker dbpediaLinker, String lang, String JAPEPATH, String hypernymLoggingPath, String taggerBinary_DE, String taggerBinary_NL) throws FileNotFoundException, GateException, MalformedURLException {
        this.lang = lang;
        this.JAPEPATH = JAPEPATH;
        this.saveInTriplets = saveInTriplets;
        this.hypernymLoggingPath = hypernymLoggingPath;
        this.taggerBinary_DE = taggerBinary_DE;
        this.taggerBinary_NL = taggerBinary_NL;
        dbpediaOutputFile = new PrintWriter(hypernymLoggingPath + ".dbpedia");
        rawOutputFile = new PrintWriter(hypernymLoggingPath + ".raw");
        this.dbpediaLinker = dbpediaLinker;
        Gate.getCreoleRegister().registerDirectories(
                new File(Gate.getPluginsHome(), "Tagger_Framework").toURL());
    }

    public void close() {
        dbpediaOutputFile.close();
        rawOutputFile.close();
    }

    public void setLoader(ProcessStatus loader) {
        this.loader = loader;
    }

    public ProcessStatus getLoader() {
        return loader;
    }

    public void extractHypernyms(Corpus corpus) {
        if (!prepared) {
            preparePipeline();
        }

        try {
            pipeline.setCorpus(corpus);
            pipeline.execute();
            for (Document doc : corpus) {
                loader.tryPrint();
                loader = loader.plusplus();
                AnnotationSet as_all = doc.getAnnotations();

                //                if (!ann_iter.hasNext()) {
//                    String articleName = (String) doc.getFeatures().get("article_title");
//                    Logger.getGlobal().log(Level.WARNING, "Article ''{0}'' no annotation ''h'' found.", articleName);
//                    //MyLogger.log(articleName + " ;NO Hypernym found");
//                }
                AnnotationSet as_hearst = as_all.get("h");
                for (Annotation isaAnnot : as_hearst) {
                    Node isaStart = isaAnnot.getStartNode();
                    Node isaEnd = isaAnnot.getEndNode();
                    String hypernym = doc.getContent().getContent(isaStart.getOffset(), isaEnd.getOffset()).toString();
                    
                    // override with lemma feature if present
                    if (isaAnnot.getFeatures().containsKey("lemma")) {
                        String hypCand = isaAnnot.getFeatures().get("lemma").toString();
                        if (hypCand != null && hypCand.length() > 0 && !hypCand.equals("<unknown>")) {
                            hypernym = hypCand;
                        }
//                        if (!hypernym.equals(hypCand)) {
//                            Logger.getGlobal().log(Level.INFO, "Replacing hypernym in text {0} with its DIFFERENT lemma: {1}", new Object[]{hypernym, hypCand});
//                        }
                    }

//                    Logger.getGlobal().log(Level.INFO, "HYPERNYM: {0}", hypernym);
                    if (hypernymLoggingPath != null && !"".equals(hypernymLoggingPath)) {
                        String dbPediaURL;
                        if (saveInTriplets) {
                            String urlName = (String) doc.getFeatures().get("dbpedia_url");
                            String urlHypernym = dbpediaLinker.getLink(hypernym);
                            if (urlHypernym == null) {
//                                Logger.getGlobal().log(Level.WARNING, "Hypernym found and not mapped: {0}", hypernym);
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

        if (mapToDBpedia == false) {
            StringBuilder content = new StringBuilder();
            content.append(source).append(";").append(hypernym);
            rawOutputFile.println(content);
        } else {
            StringBuilder content = new StringBuilder();
            content.append("<");
            content.append(StringEscapeUtils.escapeJava(source));
            content.append(">");
            //content.append(" <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ");
            content.append(" <?> ");
            content.append("<");
            content.append(StringEscapeUtils.escapeJava(hypernym));
            content.append(">.");
            dbpediaOutputFile.println(content);
        }

//            File file = new File(concreteHypernymLoggingPath);
//
//            // if file doesnt exists, then create it
//            if (!file.exists()) {
//                file.createNewFile();
//            }
//
//            FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
//            new PrintWriter(file)
//            BufferedWriter bw = new BufferedWriter(fw);
//            bw.write(content.toString());
//            bw.close();
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
                taggerFeatureMap.put("encoding", "utf-8");
//                if (!lang.equals("de")) {
//                    taggerFeatureMap.put("encoding", "utf-8");
//                } else {
//                    // for de, utf-8 causes on some documents tagger to fail
//                    taggerFeatureMap.put("encoding", "ISO-8859-1");
//                }

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
