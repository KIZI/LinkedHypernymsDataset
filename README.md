LinkedHypernymsDataset
======================

LinkedHypernymsDataset extraction framework makes RDF dataset consisting of DBpedia resources (as subjects) and types of these resources (as objects). The extraction framework returns two particular datasets LHD1.0 (types of resources are other resources or DBpedia ontology classes) and LHD2.0 (types of resources are DBpedia ontology classes only). The extraction process tries to find the hyperonymum for each DBpedia resource (HypernymExtractor module) which is transformed to another DBpedia resource (LHDOntologyCleanup module) and then is mapped to a DBpedia ontology class (LHDTypeInferrer module). Supported languages are English, German and Dutch.

## Requirements

+ Gate 7.0
+ Maven 2
+ Java 7
+ Downloaded current DBpedia datasets for the set language
  + Mapping-based Types
  + Titles
  + Short Abstracts
  + Inter-Language Links (only English dataset is required)
  + DBpedia Ontology (owl)
+ Memcached endpoint
+ 4GB RAM or more
+ Optionaly: local Wikipedia Search API (for faster processing)

## Preparation

There is a recommended file structure in the root directory:

    | Core
    | HypernymExtractor
      - module.properties                // settings of the HypernymExtractor module
    | LHDNormalizer
    | LHDOntologyCleanup
      - module.properties                // settings of the LHDOntologyCleanup module
    | LHDTypeInferrer
      - module.properties                // settings of the LHDTypeInferrer module
    | data
      | datasets
        - dbpedia_3.9.owl                // DBpedia ontology
        - instance_types_LANG.nt         // DBpedia Mapping-based Types dataset for the set language
        - interlanguage_links_en.nt      // DBpedia Inter-Language Links dataset for English
        - labels_LANG.nt                 // DBpedia Titles dataset for the set language
        - short_abstracts_LANG.nt        // DBpedia Short Abstracts dataset for the set language
      | grammar
        - de_hearst.jape                 // JAPE grammar for German
        - en_hearst.jape                 // JAPE grammar for English
        - nl_hearst.jape                 // JAPE grammar for Dutch
      | index                            
      | logs
      | output
    | utils
      | gate-7.0                         // Gate software - binary package
    - global.properties                  // global settings of all modules
    - pom.xml

Download Gate 7 software from https://gate.ac.uk/download/ (binary-only package).

Download DBpedia **Mapping-based Types dataset**, **Titles dataset** and **Short Abstracts dataset** for the set language from http://wiki.dbpedia.org/Downloads39 to the dataset directory. Datasets must be unzipped; having .nt suffix.

Download **English Inter-Language Links dataset** from http://wiki.dbpedia.org/Downloads39 to the dataset directory (the dataset must be unzipped).

Download **DBpedia Ontology (owl)** from http://wiki.dbpedia.org/Downloads39 and unzip it to the dataset directory.

Install memcached (Debian: apt-get memcached).

For a non-English language you have to download TreeTagger from http://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/ and install it. There is a special file in the gate directory plugins/Tagger_Framework/resources/TreeTagger/tree-tagger-LANG-gate which must be specified. 
+ tree-tagger-german-gate (for German)
+ tree-tagger-dutch-gate (for Dutch)


##Installation

Go to the root directory and type these Maven commands:

    mvn clean
    mvn install

After that, check the global.properties file. You have to input the absolute or relative path to key directories; any relative path begins in some used module; therefore the prefix ../ is needed to get into the root directory:

    output.dir=../data/output     # the output directory where all output files will be saved 
    logging.dir=../data/logs      # the log directory where all log files will be saved
    logging.enabled=false         # you can enable saving application logs to a file in the logs direcotory (true|false)
    lang=en                       # a set language (en|de|nl)         


##1. HypernymExtractor module

This module only extracts hypernyms from all DBpedia resources and saves them to the temporary file. Any hypernym is derived from a first sentence of a DBpedia resource short abstract and is represented by one word. Subsequently, the hypernym word is mapped to a DBpedia resource based on the first hit returned by Wikipedia Search API. The extraction process uses following Gate plugins:

1. ANNIE English Tokenizer
2. ANNIE Sentence Splitter
3. ANNIE Part-of-Speech Tagger OR TreeTagger
4. ANNIE JAPE Transducer

Before starting the extraction process, check the HypernymExtractor/module.properties file:

    global.properties.file=../global.properties                             # path to the global.properties file
    index.dir=../data/index                                                 # path to the directory where indexed datasets will be saved by Lucene
    dataset.short_abstracts.path=../data/datasets/short_abstracts_en.nt     # path to the Short Abstracts dataset for the set language
    dataset.instance_types.path=../data/datasets/instance_types_en.nt       # path to the Mapping-based Types dataset for the set language
    dataset.labels.path=../data/datasets/labels_en.nt                       # path to the Titles dataset for the set language
    gate.dir=../utils/gate-7.0                                              # path to the Gate root directory (binary package)
    gate.plugin.lhd.dir=../HypernymExtractor/target/gateplugin              # path to the compiled HypernymExtractor plugin for Gate. You needn't specify this path - don't change it!
    gate.jape.grammar=../data/grammar/en_hearst.jape                        # path to the JAPE grammar for the set language
    wiki.api=http://ner.vse.cz/wiki/                                        # Wiki Search API URL. You can use the EN mirror located on the University of Economics Prague or use original API: 
    memcached.address=192.168.116.129
    memcached.port=11211
