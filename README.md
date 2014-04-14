LinkedHypernymsDataset
======================

LinkedHypernymsDataset extraction framework makes RDF dataset consisting of DBpedia resources (as subjects) and types of these resources (as objects). The extraction framework returns two particular datasets LHD1.0 (types of resources are other resources or DBpedia ontology classes) and LHD2.0 (types of resources are DBpedia ontology classes only). The extraction process tries to find the hyperonymum for each DBpedia resource (HypernymExtractor module) which is transformed to another DBpedia resource and then is mapped to a DBpedia ontology class (LHDOntologyCleanup module and LHDTypeInferrer module). Supported languages are English, German and Dutch.

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

Download **English Inter-Language Links dataset** and **English Mapping-based Types dataset** from http://wiki.dbpedia.org/Downloads39 to the dataset directory (the datasets must be unzipped).

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
    wiki.api=http://ner.vse.cz/wiki/                                        # Wiki Search API URL. You can use the EN mirror located in the University of Economics Prague which is not limited or use original API: http://en.wikipedia.org/w/
    memcached.address=127.0.0.1                                             # Memcached server address
    memcached.port=11211                                                    # Memcached server port

After checking the properties go to the HypernymExtractor directory and start the extraction process by these commands:

    mvn scala:run -Dlauncher=indexer -DaddArgs=module.properties
    mvn scala:run -Dlauncher=runner -DaddArgs=module.properties
    
For each command you must set path to the properties file as first argument of the script launcher (-DaddArgs=module.properties). The first command indexes datasets by Lucene; the second one extracts hypernyms for all DBpedia resources and saves results to the output directory. After these steps you can continnue to the next module.

Optionaly: The extraction process can be started in parallel. You can **map** it to more processes by specifying a start pointer and final pointer; then you can **reduce** these pieces to the one result file.

    mvn scala:run -Dlauncher=runner -DaddArgs=module.properties|10000|20000       -- this command handles all resources from 10000 to 20000
    mvn scala:run -Dlauncher=stats -DaddArgs=module.properties                    -- this command shows number of all resources


##2. LHDOntologyCleanup module

This module loads results of the HypernymExtractor module where a DBpedia resource type is represented by another DBpedia resource and tries to map it to DBpedia ontology types. It is achieved by a naive ontology mapping algorithm. For each entity-linked hypernym pair, the algorithm tries to ﬁnd a DBpedia Ontology concept based on a textual match. The result is a set of files which are used in the final step making LHD datasets in the LHDTypeInferrer module.

Before starting the mapping process, check the LHDOntologyCleanup/module.properties file:

    global.properties.file=../global.properties                                          # path to the global.properties file
    dataset.instance_types.path=../data/datasets/instance_types_en.nt                    # path to the Mapping-based Types dataset depends on the set language
    dataset.instance_types.en.path=../data/datasets/instance_types_en.nt                 # path to the English Mapping-based Types dataset
    dataset.interlanguage_links.en.path=../data/datasets/interlanguage_links_en.nt       # path to the English Inter-Language Links dataset
    dataset.ontology.path=../data/datasets/dbpedia_3.9.owl                               # path to the DBpedia Ontology file
    
After checking the properties go to the LHDOntologyCleanup directory and start the mapping process by these commands (there are required result files of the HypernymExtractor process in the output directory):

    mvn scala:run -DaddArgs=module.properties


##3. LHDTypeInferrer module

This is the final step making LHD datasets. LHDTypeInferrer module tries to infer remaining DBpedia ontology types by the STI algorithm (Statistical Type Inference) which weren't mapped within the previous step.

Before starting the inferring process, check the LHDTypeInferrer/module.properties file:

    global.properties.file=../global.properties                              # path to the global.properties file
    dataset.instance_types.path=../data/datasets/instance_types_en.nt        # path to the Mapping-based Types dataset
    dataset.ontology.path=../data/datasets/dbpedia_3.9.owl                   # path to the DBpedia Ontology file
    compressTemporaryFiles=true                                              # if true then all generated temporary files will be zipped to the one file and deleted from the output directory (true|false).
    
After checking the properties go to the LHDTypeInferrer directory and start the inferring process by these commands (there are required result files of the LHDOntologyCleanup process in the output directory):

    mvn scala:run -DaddArgs=module.properties
    
##Results

The LHDTypeInferrer module made two key files: **en.LHDv1.draft.nt** and **en.LHDv2.draft.nt**. If the property 'compressTemporaryFiles' was set to 'true', all temporary files were zipped to the **en.temp.draft.zip** file and deleted from the output directory; if false, temporary files still exist in the output directory.

###LHD 1.0

The dataset contains types of all DBpedia resources represented by a DBpedia ontology type or another DBpedia resource. If any textual match of a DBpedia resource is found in the DBpedia ontology, this DBpedia resource will be mapped to a DBpedia ontology type. For example:


    # there isn't any textual match for the Republic resource in the DBpedia ontology 
    <http://dbpedia.org/resource/Germany> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/resource/Republic> .
    
    # there is some textual match for the Country resource in the DBpedia ontology. It is mapped! 
    <http://dbpedia.org/resource/Chile> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Country> .
    
    
###LHD 2.0

In this dataset, all unmapped types are assigned to a DBpedia ontology type by the STI algorithm. The object is never any DBpedia resource, it is always a DBpedia ontology type. For example:


    # Inferred DBpedia ontology type
    <http://dbpedia.org/resource/Germany> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Place> .
    
    <http://dbpedia.org/resource/Chile> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Country> .
