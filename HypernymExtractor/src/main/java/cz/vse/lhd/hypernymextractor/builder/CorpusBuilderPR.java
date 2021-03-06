package cz.vse.lhd.hypernymextractor.builder;

import cz.vse.lhd.core.lucene.NTIndexer;
import gate.Corpus;
import gate.ProcessingResource;
import gate.Resource;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ResourceInstantiationException;

/**
 * @author Milan Dojchinovski
 *         <milan (at) dojchinovski (dot) mk>
 *         Twitter:
 */
public abstract class CorpusBuilderPR extends AbstractLanguageAnalyser implements ProcessingResource {

    private Corpus corpus = null;
    private Integer startPosInArticleNameList;
    private Integer endPosInArticleNameList;
    private NTIndexer disambiguations;
    private DBpediaLinker dbpediaLinker;

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

    public Integer getStartPosInArticleNameList() {
        return startPosInArticleNameList;
    }

    public void setStartPosInArticleNameList(Integer startPosInArticleNameList) {
        this.startPosInArticleNameList = startPosInArticleNameList;
    }

    public Integer getEndPosInArticleNameList() {
        return endPosInArticleNameList;
    }

    public void setEndPosInArticleNameList(Integer endPosInArticleNameList) {
        this.endPosInArticleNameList = endPosInArticleNameList;
    }

    public NTIndexer getDisambiguations() {
        return disambiguations;
    }

    public void setDisambiguations(NTIndexer disambiguations) {
        this.disambiguations = disambiguations;
    }

    public DBpediaLinker getDbpediaLinker() {
        return dbpediaLinker;
    }

    public void setDbpediaLinker(DBpediaLinker dbpediaLinker) {
        this.dbpediaLinker = dbpediaLinker;
    }
}