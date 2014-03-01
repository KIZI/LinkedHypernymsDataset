package cz.vse.lhd.hypernymextractor;

import gate.CreoleRegister;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.ProcessingResource;
import gate.creole.SerialController;
import gate.util.GateException;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author Milan Dojchinovski
 * <milan (at) dojchinovski (dot) mk>
 * Twitter:
 * @m1ci www: http://dojchinovski.mk
 */
public class RunDefaultPipeline {

    public static void main(String args[]) throws GateException, MalformedURLException, XPathExpressionException, ParserConfigurationException, SAXException, IOException {
        

        try {
            //Gate.init(); 
            File gateHomeFile = new File(Conf.gateDir());
            Gate.setGateHome(gateHomeFile);

            File pluginsHomeFile = new File(Conf.gateDir().replaceAll("/+$", "") + "/plugins");
            Gate.setPluginsHome(pluginsHomeFile);

            Gate.init();

            URL annieHome;
            annieHome = new File(pluginsHomeFile, "ANNIE").toURL();
            Gate.getCreoleRegister().registerDirectories(annieHome);
            URL cBuilderHome;
            cBuilderHome = new File(Conf.gatePluginLhdDir()).toURL();

            CreoleRegister register = Gate.getCreoleRegister();
            register.registerDirectories(cBuilderHome);
            System.out.println("Registering  " + cBuilderHome);
            SerialController cPipeline;
            FeatureMap featureMap = Factory.newFeatureMap();

            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();

            XPathExpression xPathExpression = null;
            try {
                xPathExpression = xPath.compile("//PARAMETER");
            } catch (XPathExpressionException ex) {
                Logger.getLogger(RunDefaultPipeline.class.getName()).log(Level.SEVERE, null, ex);
            }

            DocumentBuilder builder = null;
            try {
                builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            } catch (ParserConfigurationException ex) {
                Logger.getLogger(RunDefaultPipeline.class.getName()).log(Level.SEVERE, null, ex);
            }
            org.w3c.dom.Document document = null;
            try {
                //InputStream fstream = RunDefaultPipeline.class.getResourceAsStream("creole.xml");
                // Get the object of DataInputStream
                DataInputStream in = new DataInputStream(new FileInputStream(Conf.gatePluginLhdDir() + "/creole.xml"));
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                document = builder.parse(new InputSource(br));
            } catch (SAXException ex) {
                Logger.getLogger(RunDefaultPipeline.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(RunDefaultPipeline.class.getName()).log(Level.SEVERE, null, ex);
            }
            NodeList list = null;
            try {
                list = (NodeList) xPathExpression.evaluate(document, XPathConstants.NODESET);
            } catch (XPathExpressionException ex) {
                Logger.getLogger(RunDefaultPipeline.class.getName()).log(Level.SEVERE, null, ex);
            }

            for (int i = 0; i < list.getLength(); i++) {
                String paramName = list.item(i).getAttributes().getNamedItem("NAME").getTextContent();
                String paramVal = list.item(i).getAttributes().getNamedItem("DEFAULT").getTextContent();
                featureMap.put(paramName, paramVal);
                if (paramName.equals("hypernymLoggingPath")) {
                    //MyLogger.init(paramVal + ".master");
                }
            }

            if (args.length == 2) {
                featureMap.put("startPosInArticleNameList", args[0]);
                featureMap.put("endPosInArticleNameList", args[1]);
            }

            ProcessingResource wikiPR = (ProcessingResource) Factory.createResource("cz.vse.lhd.hypernymextractor.builder.CorpusBuilderPR2", featureMap);
            cPipeline = (SerialController) Factory.createResource("gate.creole.SerialController");
            cPipeline.add(wikiPR);

            //write log message

            //MyLogger.log("Starting to process batch: from " + start + " to " + end);
            cPipeline.execute();

        } catch (MalformedURLException ex) {
            Logger.getLogger(RunDefaultPipeline.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            //MyLogger.log(success + " to process batch: from " + start + " to " + end );          
        }


    }
}
