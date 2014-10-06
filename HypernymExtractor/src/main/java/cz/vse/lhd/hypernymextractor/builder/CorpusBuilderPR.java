package cz.vse.lhd.hypernymextractor.builder;

import gate.Corpus;
import gate.ProcessingResource;
import gate.Resource;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ResourceInstantiationException;

/**
 * @author Milan Dojchinovski
 * <milan (at) dojchinovski (dot) mk>
 * Twitter:
 * @m1ci www: http://dojchinovski.mk
 */
public abstract class CorpusBuilderPR extends AbstractLanguageAnalyser implements ProcessingResource {

    private Corpus corpus = null;
    private Integer corpusSize;
    private Integer docCountStartOffset;
    private String lang;
    private Boolean firstSentenceOnly;
    private String firmArticleTitle;
    private Boolean assignDBpediaTypes;
    private Boolean saveInTriplets;
    private String specialWikiAPIURL_NL;
    private String specialWikiAPIURL_DE;
    private String specialWikiAPIURL_EN;
    private String wikiAPIBase_NL;
    private String wikiAPIBase_DE;
    private String wikiAPIBase_EN;
    private String taggerBinary_DE;
    private String taggerBinary_NL;
    private String JAPEPATH_EN;
    private String JAPEPATH_NL;
    private String JAPEPATH_DE;
    private String pathToArticleNames;
    private String hypernymLoggingPath;
    private Integer startPosInArticleNameList;
    private Integer endPosInArticleNameList;
    private String confFilePath;

    public CorpusBuilderPR() {
    }

    @Override
    public Resource init() throws ResourceInstantiationException {
        //System.out.println("Incialization");
        return super.init();
    }

    @Override
    public void reInit() throws ResourceInstantiationException {
        init();
    }

    @Override
    abstract public void execute();

    @Override
    public Corpus getCorpus() {
        return corpus;
    }

    @Override
    public void setCorpus(Corpus corpus) {
        this.corpus = corpus;
    }

    public Integer getCorpusSize() {
        return corpusSize;
    }

    public void setCorpusSize(Integer corpusSize) {
        this.corpusSize = corpusSize;
    }

    public Integer getDocCountStartOffset() {
        return docCountStartOffset;
    }

    public void setDocCountStartOffset(Integer docCountStartOffset) {
        this.docCountStartOffset = docCountStartOffset;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public Boolean getAssignDBpediaTypes() {
        return assignDBpediaTypes;
    }

    public void setAssignDBpediaTypes(Boolean assignDBpediaTypes) {
        this.assignDBpediaTypes = assignDBpediaTypes;
    }

    public String getFirmArticleTitle() {
        return firmArticleTitle;
    }

    public void setFirmArticleTitle(String debugFirmURL) {
        this.firmArticleTitle = debugFirmURL;
    }

    public Boolean getFirstSentenceOnly() {
        return firstSentenceOnly;
    }

    public void setFirstSentenceOnly(Boolean firstSentenceOnly) {
        this.firstSentenceOnly = firstSentenceOnly;
    }

    /**
     * @return the specialWikiAPIURL_NL
     */
    public String getSpecialWikiAPIURL_NL() {
        return specialWikiAPIURL_NL;
    }

    /**
     * @param specialWikiAPIURL_NL the specialWikiAPIURL_NL to set
     */
    public void setSpecialWikiAPIURL_NL(String specialWikiAPIURL_NL) {
        this.specialWikiAPIURL_NL = specialWikiAPIURL_NL;
    }

    /**
     * @return the specialWikiAPIURL_DE
     */
    public String getSpecialWikiAPIURL_DE() {
        return specialWikiAPIURL_DE;
    }

    /**
     * @param specialWikiAPIURL_DE the specialWikiAPIURL_DE to set
     */
    public void setSpecialWikiAPIURL_DE(String specialWikiAPIURL_DE) {
        this.specialWikiAPIURL_DE = specialWikiAPIURL_DE;
    }

    /**
     * @return the specialWikiAPIURL_EN
     */
    public String getSpecialWikiAPIURL_EN() {
        return specialWikiAPIURL_EN;
    }

    /**
     * @param specialWikiAPIURL_EN the specialWikiAPIURL_EN to set
     */
    public void setSpecialWikiAPIURL_EN(String specialWikiAPIURL_EN) {
        this.specialWikiAPIURL_EN = specialWikiAPIURL_EN;
    }

    /**
     * @return the JAPEPATH_EN
     */
    public String getJAPEPATH_EN() {
        return JAPEPATH_EN;
    }

    /**
     * @param JAPEPATH_EN the JAPEPATH_EN to set
     */
    public void setJAPEPATH_EN(String JAPEPATH_EN) {
        this.JAPEPATH_EN = JAPEPATH_EN;
    }

    /**
     * @return the JAPEPATH_NL
     */
    public String getJAPEPATH_NL() {
        return JAPEPATH_NL;
    }

    /**
     * @param JAPEPATH_NL the JAPEPATH_NL to set
     */
    public void setJAPEPATH_NL(String JAPEPATH_NL) {
        this.JAPEPATH_NL = JAPEPATH_NL;
    }

    /**
     * @return the JAPEPATH_DE
     */
    public String getJAPEPATH_DE() {
        return JAPEPATH_DE;
    }

    /**
     * @param JAPEPATH_DE the JAPEPATH_DE to set
     */
    public void setJAPEPATH_DE(String JAPEPATH_DE) {
        this.JAPEPATH_DE = JAPEPATH_DE;
    }

    /**
     * @return the pathToArticeNames
     */
    public String getPathToArticleNames() {
        return pathToArticleNames;
    }

    /**
     * @param pathToArticeNames the pathToArticeNames to set
     */
    public void setPathToArticleNames(String pathToArticeNames) {
        this.pathToArticleNames = pathToArticeNames;
    }

    /**
     * @return the startPosInArticleNameList
     */
    public Integer getStartPosInArticleNameList() {
        return startPosInArticleNameList;
    }

    /**
     * @param startPosInArticleNameList the startPosInArticleNameList to set
     */
    public void setStartPosInArticleNameList(Integer startPosInArticleNameList) {
        this.startPosInArticleNameList = startPosInArticleNameList;
    }

    /**
     * @return the endPosInArticleNameList
     */
    public Integer getEndPosInArticleNameList() {
        return endPosInArticleNameList;
    }

    /**
     * @param endPosInArticleNameList the endPosInArticleNameList to set
     */
    public void setEndPosInArticleNameList(Integer endPosInArticleNameList) {
        this.endPosInArticleNameList = endPosInArticleNameList;
    }

    /**
     * @return the hypernymLoggingPath
     */
    public String getHypernymLoggingPath() {
        return hypernymLoggingPath;
    }

    /**
     * @param hypernymLoggingPath the hypernymLoggingPath to set
     */
    public void setHypernymLoggingPath(String hypernymLoggingPath) {
        this.hypernymLoggingPath = hypernymLoggingPath;
    }

    /**
     * @return the wikiAPIBase_NL
     */
    public String getWikiAPIBase_NL() {
        return wikiAPIBase_NL;
    }

    /**
     * @param wikiAPIBase_NL the wikiAPIBase_NL to set
     */
    public void setWikiAPIBase_NL(String wikiAPIBase_NL) {
        this.wikiAPIBase_NL = wikiAPIBase_NL;
    }

    /**
     * @return the wikiAPIBase_DE
     */
    public String getWikiAPIBase_DE() {
        return wikiAPIBase_DE;
    }

    /**
     * @param wikiAPIBase_DE the wikiAPIBase_DE to set
     */
    public void setWikiAPIBase_DE(String wikiAPIBase_DE) {
        this.wikiAPIBase_DE = wikiAPIBase_DE;
    }

    /**
     * @return the wikiAPIBase_EN
     */
    public String getWikiAPIBase_EN() {
        return wikiAPIBase_EN;
    }

    /**
     * @param wikiAPIBase_EN the wikiAPIBase_EN to set
     */
    public void setWikiAPIBase_EN(String wikiAPIBase_EN) {
        this.wikiAPIBase_EN = wikiAPIBase_EN;
    }

    /**
     * @return the saveInTriplets
     */
    public Boolean getSaveInTriplets() {
        return saveInTriplets;
    }

    /**
     * @param saveInTriplets the saveInTriplets to set
     */
    public void setSaveInTriplets(Boolean saveInTriplets) {
        this.saveInTriplets = saveInTriplets;
    }

    /**
     * @return the taggerDir_DE
     */
    public String getTaggerBinary_DE() {
        return taggerBinary_DE;
    }

    /**
     * @param taggerDir_DE the taggerDir_DE to set
     */
    public void setTaggerBinary_DE(String taggerDir_DE) {
        this.taggerBinary_DE = taggerDir_DE;
    }

    /**
     * @return the taggerDir_NL
     */
    public String getTaggerBinary_NL() {
        return taggerBinary_NL;
    }

    /**
     * @param taggerDir_NL the taggerDir_NL to set
     */
    public void setTaggerBinary_NL(String taggerDir_NL) {
        this.taggerBinary_NL = taggerDir_NL;
    }

    public String getConfFilePath() {
        return confFilePath;
    }

    public void setConfFilePath(String confFilePath) {
        this.confFilePath = confFilePath;
    }
}
