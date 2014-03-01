package cz.vse.lhd.hypernymextractor.VAO;

import cz.vse.lhd.hypernymextractor.builder.DocumentFetcher;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
 public class Article {

    public Article(String text, String lang) {

        this.content = cleanWikiArticleText(text);
        extractSections(this.content);

        this.lang = lang;
    }
    private String firstSection;
    private String content;
    private String lang;
    private String[] headings;
    private String[] sections;
    private String type;
    private String wikipedia_url;
    private String dbpedia_url;
    private String title;

    /**
     * @return the content
     */
    public String getContent() {
        return content;
    }

    /**
     * @param content the content to set
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * @return the lang
     */
    public String getLang() {
        return lang;
    }

    /**
     * @param lang the lang to set
     */
    public void setLang(String lang) {
        this.lang = lang;
    }

    /**
     * DOC NEEDS TO BE UPDATED Parses article into sections.
     *
     * @param text Clean wikipedia text to be divided in sections, based on wiki
     * syntax
     * @return an array of sections <b>Headings are NOT currently included</b>
     */
    private void extractSections(String text) {
        //temporarily returns only Sections

        List tSections = new LinkedList();
        List sectionHeadings = new LinkedList();
        //will match text all text before a heading or end of file whatever occurs first
        Pattern firstParagraph = Pattern.compile("(.*?)($|==+.+?==+)", Pattern.DOTALL);
        Matcher SectionMatcher = firstParagraph.matcher(text);

        if (SectionMatcher.find()) {
            //Section heading causes trouble when hearst pattern is matched in the first sentence
            //of the document and the subject - the term to which hypernym is sought - is missing
            //on the other hand, when no heading is present, the entire section is skipped
            sectionHeadings.add("");
            tSections.add(SectionMatcher.group(1));
            this.firstSection = SectionMatcher.group(1);
            // System.out.println("opa: " + SectionMatcher.group(1));
            // System.out.println("end");
        }

        Pattern otherParagraphs = Pattern.compile("==+(.*?)(==+)(.*?)==+", Pattern.DOTALL);
        SectionMatcher = otherParagraphs.matcher(text);

        while (SectionMatcher.find()) {
            sectionHeadings.add(SectionMatcher.group(1));
            // SectionMatcher.group(1) is the level of title, e.g. value is "=="
            tSections.add(SectionMatcher.group(3)); //body
        }

        Pattern lastParagraph = Pattern.compile("==+([^=]*?)(==+)([^=]+)$", Pattern.DOTALL);
        SectionMatcher = lastParagraph.matcher(text);
        if (SectionMatcher.find()) {
            sectionHeadings.add(SectionMatcher.group(1));
            // SectionMatcher.group(1) is the level of title, e.g. value is "=="
            tSections.add(SectionMatcher.group(3)); //body  
        }

        this.setSections((String[]) tSections.toArray(new String[tSections.size()]));
        this.setHeadings((String[]) sectionHeadings.toArray(new String[sectionHeadings.size()]));
    }

    /**
     * Strips away any wiki syntax and non-natural language content from the
     * article text
     *
     * @param Article text in wikisyntax as returned by SpecialExport
     * @return plain text
     */
    private String cleanWikiArticleText(String text) {

        //article text is supposed to be in the wiki not html syntax
        String temp;

        //leave only the content of the text element, all other xml is deleted
        //? suggestion: remove xml:space attribute from the regex
        //(?s) je DOTALL flag - . matchuje i znak noveho radku
        //System.out.println("Running replace all... " + new Date().toString());
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();

        XPathExpression xPathExpression = null;
        try {
            xPathExpression = xPath.compile("//mediawiki/page/revision/text/text()");
        } catch (XPathExpressionException ex) {
            Logger.getLogger(DocumentFetcher.class.getName()).log(Level.SEVERE, null, ex);
        }

        //creating xml doc
        DocumentBuilder builder = null;
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(DocumentFetcher.class.getName()).log(Level.SEVERE, null, ex);
        }
        org.w3c.dom.Document document = null;
        try {
            document = builder.parse(new InputSource(new StringReader(text)));
        } catch (SAXException ex) {
            Logger.getLogger(DocumentFetcher.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(DocumentFetcher.class.getName()).log(Level.SEVERE, null, ex);
        }
        NodeList list = null;
        try {
            list = (NodeList) xPathExpression.evaluate(document, XPathConstants.NODESET);
        } catch (XPathExpressionException ex) {
            Logger.getLogger(DocumentFetcher.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (list.getLength() > 0) {
            temp = list.item(0).getTextContent();
        } else {
            temp = "";
        }

        //temp = text.replaceAll("(?s).*<text xml:space=\"preserve\">(.*)</text>.*", "$1"); //this is original
        //System.out.println("Finished replace all... " + new Date().toString());
        //System.out.println("This is new " + temp);
        //temp = text.replaceAll("(?s).*<text xml:space=\"preserve\">(.*)</text>.*", "$1");

        //remove non textual content, such as GeoBox or InfoBox
        //this might be valuable sources of information in the future
        //a one-level parenthesis recursion match {{(?:{{.*?}}|.)*?}}
        //explained at http://blog.stevenlevithan.com/archives/regex-recursion                

        //temp = temp.replaceAll("(?s)\\{{2}(?:\\{{2}.*?\\}{2}|.)*?}{2}", "");
//new code        

// end of new code
        //remove images, e.g. [[File:Delta Cultural Center-HelenaAR.jpg|thumb|Delta Cultural Center Depot]] or [[Datei:Baby-8-weeks-old.jpg|miniatur|Ein 8 Wochen alter Säugling]]


        // image caption can contain embedded link [[Datei:Ceroplastes japonicus.jpg|thumb|Kolonie von ''[[Ceroplastes japonicus]]'']] - http://de.wikipedia.org/wiki/Napfschildl%C3%A4use
        //slow temp = temp.replaceAll("\\[.*?:([^\\[\\]]*?\\|){2}(([^\\[\\]]*?(\\[\\[[^\\[\\]]+\\]\\])*[^\\[\\]]*?)*)\\]\\]", ""); // (?s)\[[^\[\]]*?\]        
        //the regex was made fast with possesive quantifiers
        temp = temp.replaceAll("\\[\\[[^\\[\\]|:]*+:[^\\[\\]s|]*+([^\\[\\]|]*+\\|){2}(([^\\[\\]]*+(\\[\\[[^\\[\\]]++\\]\\])*+)*+)\\]\\]", ""); // (?s)\[[^\[\]]*?\]
        //remove links, i.e. replace [[Chai rperson|Chairman]] to Chairman
        int lengthBeforeReplace;
        do //seems that the cycle here is not needed, one iteration would do
        {
            lengthBeforeReplace = temp.length();
            temp = temp.replaceAll("(?s)\\{\\{[^\\{\\{]*?\\}\\}", ""); // (?s)\{\{[^\{\{]*?\}\}
        } while (lengthBeforeReplace != temp.length());

        temp = temp.replaceAll("(?s)\\[\\[[^\\[\\]]*\\|(.*?)\\]\\]", "$1"); // (?s)\[\[[^\[\]]*\|(.*?)\]\]


//the following two are optional 
        temp = temp.replaceAll("(?s)\\[\\[(.*?)\\]\\]", "$1"); //remove emphasized text // (?s)\[\[(.*?)\]\]
        temp = temp.replaceAll("(?s)'''(.*?)'''", "$1"); //remove emphasized text
//delete &quot;  (or could be also replaced by "
        temp = temp.replaceAll("(?s)&quot;", "");

        //delete any references in the text (i.e. footnotes)
        // this is original -> temp = temp.replaceAll("(?s)<ref>.*?</ref>", "");
        temp = temp.replaceAll("(?s)<ref[^<>]*?>.*?</ref>", "");
        temp = temp.replaceAll("(?s)<ref[^<>]*?/>", "");
        temp = temp.replaceAll("(?s)<span[^<>]*?/>", "");
        temp = temp.replaceAll("(?s)&lt;ref&gt;.*?&lt;/ref&gt;", "");//a variant of the former

        //delete any comments such as <!-- Infobox begins !-->       
        //suggestion - should not all <.*?> be replaced?
        temp = temp.replaceAll("(?s)<!--.*?-->", "");
        temp = temp.replaceAll("(?s)&lt;!--.*?--&gt;", ""); //a variant of the former

        //delete any html(?) defined boxes
        //removing single curly braces is safe, because double curly braces have already been removed
        temp = temp.replaceAll("(?s)\\{[^\\{\\}]*?\\}", "");    //    (?s)\{[^\{\}]*?\}
        //remove any empty braces which occured due to previous removals of other markup
        temp = temp.replaceAll("(?s)\\(\\)", ""); // (?s)\(\)
        //remove links
        temp = temp.replaceAll("(?s)\\[[^\\[\\]]*?\\]", ""); // (?s)\[[^\[\]]*?\]

        temp = temp.replaceAll("&nbsp;", " ");
        temp = temp.replaceAll("__NOTOC__", "");
        //remove braces with info, e.g.  (* 11. Mai 1933 in Kassel; † 21. April 1985 in der bayerischen Benediktenwand)
        temp = temp.replaceAll("\\([^()]*?\\)", "");
        temp = temp.trim();

        return temp;
    }

    /**
     * @return the headings
     */
    public String[] getHeadings() {
        return headings;
    }

    /**
     * @param headings the headings to set
     */
    public void setHeadings(String[] headings) {
        this.headings = headings;
    }

    /**
     * @return the sections
     */
    public String[] getSections() {
        return sections;
    }

    /**
     * @param sections the sections to set
     */
    public void setSections(String[] sections) {
        this.sections = sections;
    }

    /**
     * @return the firstSection
     */
    public String getFirstSection() {
        return firstSection;
    }

    /**
     * @param firstSection the firstSection to set
     */
    public void setFirstSection(String firstSection) {
        this.firstSection = firstSection;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the wikipedia_url
     */
    public String getWikipedia_url() {
        return wikipedia_url;
    }

    /**
     * @param wikipedia_url the wikipedia_url to set
     */
    public void setWikipedia_url(String wikipedia_url) {
        this.wikipedia_url = wikipedia_url;
    }

    /**
     * @return the dbpedia_url
     */
    public String getDbpedia_url() {
        return dbpedia_url;
    }

    /**
     * @param dbpedia_url the dbpedia_url to set
     */
    public void setDbpedia_url(String dbpedia_url) {
        this.dbpedia_url = dbpedia_url;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }
}
