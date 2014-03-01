package cz.vse.lhd.hypernymextractor.builder;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.util.FileManager;
import cz.vse.lhd.hypernymextractor.VAO.Article;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Corpus;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.Node;
import gate.ProcessingResource;
import gate.creole.ResourceInstantiationException;
import gate.creole.SerialAnalyserController;
import gate.util.GateException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Milan Dojchinovski
 * <milan (at) dojchinovski (dot) mk>
 * Twitter:
 * @m1ci www: http://dojchinovski.mk
 */
public class DocumentFetcher {

    public static DocumentFetcher documentFetcher = null;
    public static SerialAnalyserController pipeline = null;
    public static boolean prepared = false;
    public static String defaultSpecialExportURL = "http://en.wikipedia.org/wiki/Special";
    private static String specialExportURL;
    private Corpus tempCorpus;
    int MINFIRSTSENTENCELENGTH = 50;
    private static boolean isInitialized = false;

    public static void init(String _specialExportURL) {
        specialExportURL = _specialExportURL;

        isInitialized = true;
    }

    public static DocumentFetcher getInstance() {
        if (!isInitialized) {
            Logger.getLogger(DocumentFetcher.class.getName()).log(Level.SEVERE, null, "Run init first");
            return null;
        }
        if (documentFetcher == null) {
            try {
                documentFetcher = new DocumentFetcher();
                //File gateHomeFile = new File("/Applications/GATE_Developer_7.0/");
                //Gate.setGateHome(gateHomeFile);

                //File pluginsHome = new File("/Applications/GATE_Developer_7.0/plugins");
                //Gate.setPluginsHome(pluginsHome);

                //URL annieHome = null;            
                //annieHome  =new File(pluginsHome, "ANNIE").toURL();

                Gate.init();

                //CreoleRegister register = Gate.getCreoleRegister();
                //register.registerDirectories(annieHome);
                // prepare the pipeline
                prepareSentenceExtractPipeline();

            } catch (GateException ex) {
                Logger.getLogger(DocumentFetcher.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return documentFetcher;
    }

    public static void prepareSentenceExtractPipeline() {
        if (!prepared) {
            try {
                prepared = true;

                pipeline = (SerialAnalyserController) Factory.createResource("gate.creole.SerialAnalyserController");

                FeatureMap tokenizerFeatureMap = Factory.newFeatureMap();
                ProcessingResource tokenizerPR = (ProcessingResource) Factory.createResource("gate.creole.tokeniser.DefaultTokeniser", tokenizerFeatureMap);

                FeatureMap sentenceSplitterFeatureMap = Factory.newFeatureMap();
                ProcessingResource sentenceSplitterPR = (ProcessingResource) Factory.createResource("gate.creole.splitter.RegexSentenceSplitter", sentenceSplitterFeatureMap);
                //ProcessingResource sentenceSplitterPR = (ProcessingResource) Factory.createResource("gate.creole.splitter.SentenceSplitter", sentenceSplitterFeatureMap);

                pipeline.add(tokenizerPR);
                pipeline.add(sentenceSplitterPR);

            } catch (ResourceInstantiationException ex) {
                Logger.getLogger(DocumentFetcher.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    public boolean fetch(Integer corpusSize, Integer docCountStartOffset, Boolean assignDBpediaTypes, String lang, Boolean firstSentenceOnly, Corpus corpus, String[] articleList) throws Exception {

        Logger.getLogger(DocumentFetcher.class.getName()).log(Level.INFO, "==== Started fetching documents ====");
        int maxAttempts;
        if (articleList != null && articleList.length != 0) {

            if (articleList.length != corpusSize) {
                Logger.getLogger(DocumentFetcher.class.getName()).log(Level.INFO, "Corpus size {0} does not match the length of the predefined list {1}, overriding corpus size to the list length", new Object[]{corpusSize, articleList.length});
                corpusSize = articleList.length;
            }
            // sometimes, the HTTP requests fails due to error (overload) on the server side
            maxAttempts = 3;
        } else {
            maxAttempts = 5;
        }

        int article_counter = 0;
        int attempts = 0;

        while (article_counter != corpusSize) {
            System.out.println(attempts);
            if (attempts == maxAttempts) {
                if (articleList != null) {
                    Logger.getLogger(DocumentFetcher.class.getName()).log(Level.INFO, "Failed to retrieve article {0} moving to next article", articleList[article_counter]);
                    //MyLogger.log(articleList[article_counter] + "; SKIPPED - other failure");
                    article_counter++;
                    attempts = 0;
                    continue;
                } //for random article retrieval, it makes sense only to quit
                else {
                    Logger.getLogger(DocumentFetcher.class.getName()).log(Level.INFO, "Exhausting max attempts and quitting");
                    break;
                }
            }
            attempts++;
            try {
                Article article;


                if (articleList == null) {
                    article = getRandomArticle(lang, "");
                } else {
                    article = getRandomArticle(lang, articleList[article_counter]);
                }


                if (article.getType().equals("normal_article")) {
                    //remove after
                    // article.setDbpedia_url("http://de.dbpedia.org/resource/Dornröschen_(2009)");
                    // article.setTitle("Dornröschen (2009)");

                    Document doc;


                    doc = Factory.newDocument(article.getFirstSection());


                    // Document doc = Factory.newDocument(buffer.toString());
                    doc.setName("doc-" + (docCountStartOffset + article_counter));

                    // set wikipedia disambiguation URL
                    doc.getFeatures().put("article_title", article.getTitle());

                    // set wikipedia disambiguation URL
                    doc.getFeatures().put("wikipedia_url", article.getWikipedia_url());

                    doc.getFeatures().put("dbpedia_url", article.getDbpedia_url());
                    if (assignDBpediaTypes) {
                        // set dbpedia disambiguation URL

                        Model model = ModelFactory.createDefaultModel();

                        InputStream in = null;

                        if (lang.equals("en")) {
                            in = FileManager.get().open(new URI("http", "dbpedia.org", "/resource/" + article.getTitle().replace(" ", "_"), null).toASCIIString());
                        } else if (lang.equals("de")) {
                            in = FileManager.get().open(new URI("http", "de.dbpedia.org", "/resource/" + article.getTitle().replace(" ", "_"), null).toASCIIString());
                        } else if (lang.equals("nl")) {
                            in = FileManager.get().open(new URI("http", "nl.dbpedia.org", "/resource/" + article.getTitle().replace(" ", "_"), null).toASCIIString());
                        }

                        if (in == null) {
                            throw new IllegalArgumentException("File: not found");
                        }

                        // read the RDF/XML file
                        model.read(in, null);
                        // model.write(System.out);
                        System.out.println("===");
                        System.out.println("RDF: " + article.getDbpedia_url());
                        // http://de.dbpedia.org/resource/Parkh%C3%B6hle
                        // http://de.dbpedia.org/resource/Dornröschen_(2009)
                        String q = null;

                        if (lang.equals("en")) {
                            q = "SELECT ?o WHERE {"
                                    + "<" + new URI("http", "dbpedia.org", "/resource/" + article.getTitle().replace(" ", "_"), null).toURL().toString() + ">" + " <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?o . }";
                        } else if (lang.equals("de")) {
                            q = "SELECT ?o WHERE {"
                                    + "<" + new URI("http", "de.dbpedia.org", "/resource/" + article.getTitle().replace(" ", "_"), null).toURL().toString() + ">" + " <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?o . }";
                        } else if (lang.equals("nl")) {
                            q = "SELECT ?o WHERE {"
                                    + "<" + new URI("http", "nl.dbpedia.org", "/resource/" + article.getTitle().replace(" ", "_"), null).toURL().toString() + ">" + " <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?o . }";
                        }

                        System.out.println(q);
                        System.out.println("= DB TYPES =");
                        Query query = QueryFactory.create(q);
                        QueryExecution qe = QueryExecutionFactory.create(query, model);
                        ResultSet results = qe.execSelect();
                        int counter = 0;
                        while (results.hasNext()) {
                            QuerySolution row = results.next();
                            RDFNode thing = row.get("o");
                            System.out.println(thing.toString());
                            doc.getFeatures().put("db_type_" + counter, thing.toString());
                            counter++;
                        }
                        System.out.println("===");

                    }

                    // set language feature
                    doc.getFeatures().put("lang", lang);

                    corpus.add(doc);

                } else {
                    Logger.getLogger(DocumentFetcher.class.getName()).log(Level.INFO, "Wrong type {0} for article {1} moving to next article", new Object[]{article.getType(), article.getTitle()});
                    //MyLogger.log(article.getTitle() + "; SKIPPED - TYPE:" + article.getType());
                }
                article_counter++;
                attempts = 1;

            } catch (Exception ex) {
                //ex.printStackTrace();
                Logger.getLogger(DocumentFetcher.class.getName()).log(Level.SEVERE, null, "Connection exception for " + articleList[article_counter]);
                Logger.getLogger(DocumentFetcher.class.getName()).log(Level.SEVERE, null, ex);
                continue;
            }
        }

        System.out.println("==DOCUMENT RETRIEVAL FINISHED==");
        if (corpus.size() == 0) {
            System.out.println("ADDITIONAL PROCESSING SKIPPED - CORPUS EMPTY");
            return false;
        }
        if (firstSentenceOnly) {
            System.out.println("NOW EXTRACT FIRST SENTENCE WILL START");
            pipeline.setCorpus(corpus);
            pipeline.execute();
            // System.out.println("size of corpus: " + corpus.size());
            for (Document doc : corpus) {

                AnnotationSet as_all = doc.getAnnotations();
                AnnotationSet as_sentences = as_all.get("Sentence");
                String s = "";
                for (Annotation sentenceAnnotation : as_sentences.get("Sentence")) {

                    Node isaStart = sentenceAnnotation.getStartNode();
                    Node isaEnd = sentenceAnnotation.getEndNode();
                    //System.out.println(isaStart.getOffset());
                    s = doc.getContent().getContent(isaStart.getOffset(), isaEnd.getOffset()).toString();
                    //System.out.println("Sentence: " + s);
                    if (isaStart.getOffset() == 0 || isaStart.getOffset() == 2) {
                        doc.setContent(doc.getContent().getContent(isaStart.getOffset(), isaEnd.getOffset()));
                        break;
                    }
                }
            }
        }


        System.out.println("NOW HYPERNYM EXTRACTOR WILL START");
        HypernymExtractor hypExtractor = HypernymExtractor.getInstance();
        hypExtractor.extractHypernyms(corpus);

        return true;
    }

//    public Article getArticleFromDBpediaDatasets(String lang, String subject) {
//        
//    }
    
    public Article getRandomArticle(String lang, String firmArticleTitle) {
        try {
            URL url;

            HttpURLConnection.setFollowRedirects(true);
            if (firmArticleTitle != null && !firmArticleTitle.equals("")) {
                url = new URL(specialExportURL + firmArticleTitle);
            } else {
                url = new URL(specialExportURL.substring(0, specialExportURL.lastIndexOf(":")) + ":Random");
            }


            // HttpURLConnection.setFollowRedirects(false);
            URLConnection connection = url.openConnection();
            // String locationHeader = connection.getHeaderField("Location");
            InputStream tempis = connection.getInputStream();
            tempis = null;
            System.out.println(connection.getURL());
            int slashIndex = connection.getURL().toString().lastIndexOf('/');
            String docTitle = connection.getURL().toString().substring(slashIndex + 1);

            // HttpURLConnection.setFollowRedirects(true);
            URL artURL;

            System.out.println(specialExportURL + docTitle);
            artURL = new URL(specialExportURL + docTitle);

            URLConnection artURLConnection = artURL.openConnection();
            StringBuilder buffer = new StringBuilder();

            InputStream is = artURLConnection.getInputStream();
            Reader isr = new InputStreamReader(is, "UTF-8");
            Reader in = new BufferedReader(isr);

            int ch;

            while ((ch = in.read()) > -1) {
                buffer.append((char) ch);
            }
            in.close();


            //possible performance optimization: do not create article before it is known that it is normal article
            Article article = new Article(buffer.toString(), lang);

            // set default type to all, this may be overriden later          
            article.setType("normal_article");

            //!!! do not use  article.getContent(), because it is already stripped down of wiki syntax!
            String content = buffer.toString().toLowerCase();

            /*if (debugFirmURL!=null & debugFirmURL!="")
             {
             Logger.getLogger(DocumentFetcher.class.getName()).log(Level.INFO, "Fulltext (before clean):" + content);
             }*/
            if (article.getFirstSection().trim().length() < 5) {
                article.setType("emptyarticle");
            } else if (content.contains("#redirect") | content.contains("<redirect")) {
                article.setType("redirect");
            } else if (lang.equals("en")) {
                if (docTitle.contains("(disambiguation)")
                        | content.contains("{{disamb")
                        | content.contains("{{dab")
                        | content.matches("\\{\\{[^{}]{0,50}?disambiguation[^{}]{0,50}?\\}\\}")
                        | content.contains("{{geodis}}") | article.getContent().contains("{{hndis")
                        | content.contains("{{numberdis")) {
                    article.setType("disambiguation_article");
                } //http://en.wikipedia.org/wiki/Wikipedia:Manual_of_Style_%28disambiguation_pages%29#The_disambig_notice_and_categorization
                //http://en.wikipedia.org/wiki/Template:Disambig
                else if (docTitle.startsWith("List ") | docTitle.startsWith("List_") | content.startsWith("this is a list")
                        | article.getContent().startsWith("this is a list")) {
                    article.setType("list");
                } else if (article.getContent().contains("{{roadindex")
                        | article.getContent().contains("{{mountainindex")
                        | article.getContent().contains("{{shipindex")
                        | article.getContent().contains("{{sportindex")
                        | article.getContent().contains("{{surname")
                        | article.getContent().contains("{{given name")) {
                    article.setType("index");
                }
            } else if (lang.equals("de")) {
                if (docTitle.contains("(Begriffsklärung)")
                        | content.contains("{{begriffskl")) {
                    article.setType("disambiguation_article");
                } else if (docTitle.startsWith("Liste ")
                        | docTitle.startsWith("Liste_")) {
                    article.setType("list");
                }
            } else if (lang.equals("nl")) {
                if (content.contains("#doorverwijzing [[") | content.contains("#doorverwijzing[[")) {
                    article.setType("redirect");
                }
                if (docTitle.contains("Lijst ")
                        | docTitle.contains("Lijst_")) {
                    {
                        article.setType("list");
                    }
                } else if (content.contains("{{dp}}")) {
                    article.setType("disambiguation_article");
                }

            }
            if (!article.getType().equals("normal_article")) {
                Logger.getLogger(DocumentFetcher.class.getName()).log(Level.INFO, "Article {0} has special type: {1} (should be skipped)", new Object[]{docTitle, article.getType()});
            }

            // setting Wikipedia url
            if (lang.equals("en")) {
                article.setWikipedia_url("http://en.wikipedia.org/wiki/" + java.net.URLDecoder.decode(docTitle, "UTF-8"));
            } else if (lang.equals("de")) {
                article.setWikipedia_url("http://de.wikipedia.org/wiki/" + java.net.URLDecoder.decode(docTitle, "UTF-8"));
            } else if (lang.equals("nl")) {
                article.setWikipedia_url("http://nl.wikipedia.org/wiki/" + java.net.URLDecoder.decode(docTitle, "UTF-8"));
            }

            // setting DBpedia url
            if (lang.equals("en")) {
                article.setDbpedia_url("http://dbpedia.org/resource/" + java.net.URLDecoder.decode(docTitle, "UTF-8"));
            } else if (lang.equals("de")) {
                article.setDbpedia_url("http://de.dbpedia.org/resource/" + java.net.URLDecoder.decode(docTitle, "UTF-8"));
            } else if (lang.equals("nl")) {
                article.setDbpedia_url("http://nl.dbpedia.org/resource/" + java.net.URLDecoder.decode(docTitle, "UTF-8"));
            }

            System.out.println(article.getDbpedia_url());
            // setting article title
            article.setTitle(java.net.URLDecoder.decode(docTitle.replace("_", " "), "UTF-8"));

            return article;

        } catch (IOException ex) {
            Logger.getLogger(DocumentFetcher.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
