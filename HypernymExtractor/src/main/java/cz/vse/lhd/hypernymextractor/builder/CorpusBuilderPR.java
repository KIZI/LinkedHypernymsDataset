package cz.vse.lhd.hypernymextractor.builder;

import gate.Corpus;
import gate.ProcessingResource;
import gate.Resource;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ResourceInstantiationException;

/**
 * @author Milan Dojchinovski
 *         <milan (at) dojchinovski (dot) mk>
 *         Twitter:
 * @m1ci www: http://dojchinovski.mk
 */
public abstract class CorpusBuilderPR extends AbstractLanguageAnalyser implements ProcessingResource {

    private Corpus corpus = null;
    private String taggerBinary_DE;
    private String taggerBinary_NL;
    private String JAPEPATH_EN;
    private String JAPEPATH_NL;
    private String JAPEPATH_DE;
    private Integer startPosInArticleNameList;
    private Integer endPosInArticleNameList;

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

}