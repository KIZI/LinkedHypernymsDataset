LinkedHypernymsDataset
======================

LinkedHypernymsDataset extraction framework makes RDF dataset consists of DBpedia resources (as subjects) and types of the DBpedia resources (as objects). The extraction framework returns two particular datasets LHD1.0 (types of resources are other resources or DBpedia ontology classes) and LHD2.0 (types of resources are DBpedia ontology classes only). The extraction process tries to find the hyperonymum for each DBpedia resource (HypernymExtractor module) which is transformed to another DBpedia resource (LHDOntologyCleanup module) and then is mapped to a DBpedia ontology class (LHDTypeInferrer module). Supported languages are English, German and Dutch.

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
