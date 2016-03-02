LinkedHypernymsDataset
======================

LinkedHypernymsDataset extraction framework makes RDF dataset consisting of DBpedia resources (as subjects) and types of these resources (as objects). The extraction framework returns several datasets:

+ **Core** - types of resources are DBpedia ontology classes built on Extension dataset and a hypernym pattern matching (most accurate, most specific)
+ **Inference** - types of resources are DBpedia ontology classes build on the Extension dataset and a statistical type inference algorithm (less accurate, less specific)
+ **Extension** - types of resources are other resources build on the Raw dataset and the first hypernym word hit from wikipedia API (highest type specificity)
+ **Raw** - all hypernyms are string literals extracted from the first sentence of a wikipedia resource abstract.

The extraction process tries to find the hyperonymum for each DBpedia resource (HypernymExtractor module) which is transformed to another DBpedia resource and then is mapped to a DBpedia ontology class (OntologyCleanup module and TypeInferrer module). Supported languages are English, German and Dutch.

## Requirements

+ Gate 8.0
+ Maven 2+
+ Java 8
+ Downloaded current DBpedia datasets for the set language (it is possible to use the Downloader module).
  + Mapping-based Types (for english and the set language)
  + Mapping-based Types transitive (for english and the set language)
  + Short Abstracts (for the set language)
  + Disambiguations (for the set language)
  + Inter-Language Links (only english dataset is required)
  + DBpedia Ontology (owl)
+ Memcached endpoint
+ 4GB RAM or more

## Preparation

First download the current version of LHD extraction framework:

    git clone https://github.com/KIZI/LinkedHypernymsDataset.git
    cd LinkedHypernymsDataset
    git fetch

There is a recommended file structure in the root directory:

    * Core
    * HypernymExtractor
    * OntologyCleanup
    * TypeInferrer
    * Downloader
    * data
      * datasets
        * dbpedia_2015.owl                      // DBpedia ontology
        * instance_types_LANG.nt                // DBpedia Mapping-based Types dataset for the set language
        * instance_types_en.nt                  // DBpedia Mapping-based Types dataset for the english language
        * instance_types_transitive_LANG.nt     // DBpedia Mapping-based Types transitive dataset for the set language
        * instance_types_tansitive_en.nt        // DBpedia Mapping-based Types transitive dataset for the english language
        * interlanguage_links_en.nt             // DBpedia Inter-Language Links dataset for English
        * disambiguations_LANG.nt               // DBpedia Disambiguations dataset for the set language
        * short_abstracts_LANG.nt               // DBpedia Short Abstracts dataset for the set language
        * exclude-types                         // Handwritten rules - excluded types (optional)
        * override-types                        // Handwritten rules - mappings of types to another one (optional)
      * grammar
        * de_hearst.jape                        // JAPE grammar for German
        * en_hearst.jape                        // JAPE grammar for English
        * nl_hearst.jape                        // JAPE grammar for Dutch
      * index                            
      * logs
      * output
    * utils
      * gate-8.0                                // Gate software - binary package
      * treetagger                              // Treetagger - POS tagger for German and Dutch
    * application.LANG.conf                     // settings of all modules for the set language
    * run-all.sh                                // main launcher
    * pom.xml

Download Gate 8 software from https://gate.ac.uk/download/ (binary-only package).

Install memcached (Debian: apt-get memcached).

You can download required datasets manually or use the Downloader module (see installation steps). If you want to download datasets manually, you will find all in the DBpedia homepage:
+ Download DBpedia **Mapping-based Types dataset**, **Mapping-based Types transitive dataset**, **Disambiguations dataset** and **Short Abstracts dataset** for the set language from http://wiki.dbpedia.org/Downloads to the dataset directory. Datasets must be unzipped; having .nt suffix.
+ Download **English Inter-Language Links dataset**, **English Mapping-based Types dataset** and **English Mapping-based Types transitive dataset** from http://wiki.dbpedia.org/Downloads to the dataset directory (the datasets must be unzipped).
+ Download **DBpedia Ontology (owl)** from http://wiki.dbpedia.org/Downloads and unzip it to the dataset directory.

For a non-English language you have to download TreeTagger from http://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/ and install it. There is a special file in the gate directory plugins/Tagger_Framework/resources/TreeTagger/tree-tagger-LANG-gate which must be specified and be targeted to the installed TreeTagger application (this file is generated during the TreeTagger installation step in the cmd/ directory).

+ tree-tagger-german-gate (for German)
+ tree-tagger-dutch-gate (for Dutch)

Example for German (tree-tagger-german-gate):

```
#!/bin/sh

SCRIPT_DIR="$( cd "$( dirname "$0" )" && pwd )"

OPTIONS="-token -lemma -sgml"

BIN=$SCRIPT_DIR/../../../../../treetagger/bin
CMD=$SCRIPT_DIR/../../../../../treetagger/cmd
LIB=$SCRIPT_DIR/../../../../../treetagger/lib

TOKENIZER=${CMD}/utf8-tokenize.perl
TAGGER=${BIN}/tree-tagger
ABBR_LIST=${LIB}/german-abbreviations-utf8
PARFILE=${LIB}/german-utf8.par
LEXFILE=${LIB}/german-lexicon-utf8.txt
FILTER=${CMD}/filter-german-tags

$TOKENIZER -a $ABBR_LIST $* |
# external lexicon lookup
perl $CMD/lookup.perl $LEXFILE |
# tagging
$TAGGER $OPTIONS $PARFILE | 
# error correction
$FILTER
```

Example for Dutch (tree-tagger-dutch-gate):

```
#!/bin/sh

SCRIPT_DIR="$( cd "$( dirname "$0" )" && pwd )"

OPTIONS="-token -lemma -sgml"

BIN=$SCRIPT_DIR/../../../../../treetagger/bin
CMD=$SCRIPT_DIR/../../../../../treetagger/cmd
LIB=$SCRIPT_DIR/../../../../../treetagger/lib

TOKENIZER=${CMD}/utf8-tokenize.perl
TAGGER=${BIN}/tree-tagger
ABBR_LIST=${LIB}/dutch-abbreviations
PARFILE=${LIB}/dutch-utf8.par

$TOKENIZER -a $ABBR_LIST $* |
# tagging
$TAGGER $OPTIONS $PARFILE
```

##Getting started

Before starting of the extraction process, the config file should be specified, see Installation and Modules paragraphs.

It is possible to use a shell script "run-all.sh" for starting of all processes which are needed to generate LHD dataset. This script fetches the current version of LHD extraction framework by the git command, install it by the maven command, download required datasets, remove old output files and launch the extraction process. This process can take several days therefore it should be run as a background process:

    ./run-all.sh ../application.LANG.conf > output.log 2>&1 &

Or you can use the Pipeline module where all computational processes are integrated. Go to the Pipeline directory and run all with one maven command (this module doesn't involve any datasets download step; so download datasets manually or by using the download module):

    mvn scala:run -DaddArgs="../application.LANG.conf|<skipped-tasks>|<remove-all>" > output.log 2>&1 &

Within this command you can use some optional parameters:

  * **remove-all**: if you use "remove-all" string as a second or third parameter, then the output directory will be completely cleaned before running of the extraction process.
```
mvn scala:run -DaddArgs="../application.LANG.conf|remove-all"
```
  * **skipped-tasks**: there are special flags which can be used for skipping of some extraction tasks. Within this option you can use any combination of these flags.
    * x: Skip the indexing task of the hypernym extraction process
    * e: Skip the hypernym extraction process
    * y: Skip the indexing task of the ontology cleanup
    * c: Skip the ontology cleanup task
    * z: Skip the indexing task of the STI algorithm
    * y: Skip the STI processing (Statistical Type Inferrence)
    * f: Skip the final datasets making tasks (this task aggregates outputs from all modules)
```
mvn scala:run -DaddArgs="../application.LANG.conf|xe"    # run all tasks except the hypernym extraction process with indexing
```

If some task fails then the process will continue where it left off after restart, unless you use "remove-all" parameter.

Moreover you can launch the extraction process step by step. See following paragraphs.

##Installation

Go to the root directory and type these Maven commands:

    mvn clean
    mvn install

After that, check the main config file. You have to input the absolute or relative path to the key directories; any relative path begins in some used module; therefore the prefix ../ is needed to get into the root LHD directory:

Example of the main config file (for EN):

```
LHD {
  output.dir = "../data/output"                                            # the output directory where all output files will be saved
  datasets.dir = "../data/datasets"                                        # the dataset directory
  lang = "en"                                                              # a set language (en|de|nl)
  dbpedia.version = "2015-10"                                              # DBpedia version
  HypernymExtractor {
      index-dir = "../data/index"                                          # path to the directory where indexed datasets
      wiki-api = "https://en.wikipedia.org/w/"                             # Wiki Search API URL. You can use your own mirror located in your localhost which is not limited, or use the original API (en: https://en.wikipedia.org/w/, de: https://de.wikipedia.org/w/, nl: https://nl.wikipedia.org/w/)
      gate {                  
          dir = "../utils/gate-8.0-build4825"                              # path to the Gate root directory (binary package)
          plugin-lhd-dir = "../HypernymExtractor/target/gateplugin"        # path to the compiled HypernymExtractor plugin for Gate. You needn't specify this path - don't change it!
          jape-grammar = "../data/grammar/en_hearst.jape"                  # path to the JAPE grammar for the set language
      }
      memcached {
         address = "127.0.0.1"                                             # Memcached server address
         port = 11211                                                      # Memcached server port
      }
      parallelism-level = 2                                                # number of maximal parallel threads created during the extraction process (too many threads can cause the memory leak, default is unlimited-automatic)
      corpus-size-per-thread = 20000                                       # number of resources which are being processed within one thread (default is 10000)
      maven-cmd = """"C:\Program Files (x86)\JetBrains\IntelliJ IDEA\plugins\maven\lib\maven3\bin\mvn.bat""""      # a path to the maven script (dafault is "mvn")
  }
  OntologyCleanup {
     index-dir = "../data/index"
     manualmapping {
          overridetypes-path = "../data/datasets/override-types_en"        # path to the file where handwritten rules - excluded types are saved (this is an optional setting)
          excludetypes-path = "../data/datasets/exclude-types"             # path to the file where handwritten rules, mappings of types to another one, are saved (this is an optional setting)
      }
  }
  TypeInferrer {
     index-dir = "../data/index"                                           # path to the directory where datasets will be indexed
  }
  Downloader {
        base-url = "http://downloads.dbpedia.org/2015-10/core-i18n/"       # base url where dbpedia datasets are placed
        ontology-dir = "../"                                               # a relative path from the base-url to a directory where the dbpedia ontology is placed
  }
}
```

If there are no downloaded datasets in your local computer you can use the Downloader module. Go to the Downloader module folder and type this command (all required datasets will be downloaded to the datasets directory and be normalized - we recommend to use this function instead of the manual downloading):

    mvn scala:run -DaddArgs=../application.LANG.conf

##1. HypernymExtractor module

This module extracts hypernyms from DBpedia resources with an abstract and saves them to the temporary file. Any hypernym is derived from a first sentence of a DBpedia resource short abstract and is represented by one word. Subsequently, the hypernym word is mapped to a DBpedia resource based on the first hit returned by Wikipedia Search API. The extraction process uses following Gate plugins:

1. ANNIE English Tokenizer
2. ANNIE Sentence Splitter
3. ANNIE Part-of-Speech Tagger OR TreeTagger
4. ANNIE JAPE Transducer

Before starting the extraction process, check the HypernymExtractor section in the main config file (application.LANG.conf). After checking the properties go to the HypernymExtractor directory and start the extraction process by these commands:

    mvn scala:run -DaddArgs="../application.LANG.conf|index"
    mvn scala:run -DaddArgs=../application.LANG.conf
    
For each command you must set path to the config file as the first argument of the script launcher (-DaddArgs=../application.LANG.conf). The first command indexes datasets by Lucene; the second one extracts hypernyms for all DBpedia resources and saves results to the output directory. After these steps you can continnue to the next module.

Note: The extraction process will be started in parallel, where each thread is just one maven command with some specific offset and limit. If you want to test this extraction process on some small part of dataset within one thread you can run it by this command

    mvn scala:run -DaddArgs="../application.LANG.conf|10000|10000"       -- this command handles all resources from 10000 to 20000

##2. OntologyCleanup module

This module loads results of the HypernymExtractor module where a DBpedia resource type is represented by another DBpedia resource and tries to clean and map it to a DBpedia ontology type. It is achieved by a naive ontology mapping algorithm. For each entity-linked hypernym pair, the algorithm tries to ﬁnd a DBpedia Ontology concept based on a textual match.

Before starting the mapping process, check the OntologyCleanup section in the main config file (application.LANG.conf). After checking the properties go to the OntologyCleanup directory and start the mapping process by these commands (there are required result files of the HypernymExtractor process in the output directory):

    mvn scala:run -DaddArgs="../application.LANG.conf|index"
    mvn scala:run -DaddArgs="../application.LANG.conf" 


##3. TypeInferrer module

TypeInferrer module tries to infer a DBpedia ontology type from a DBpedia resource by the STI algorithm (Statistical Type Inference).

Before starting the inferring process, check the TypeInferrer section in the main config file (application.LANG.conf). After checking the properties go to the TypeInferrer directory and start the inferring process by these commands (there are required result files of the OntologyCleanup process in the output directory):

    mvn scala:run -DaddArgs="../application.LANG.conf|index"
    mvn scala:run -DaddArgs=../application.LANG.conf 

##Results

After the Pipeline module successfully finished its work you should see four basic datasets in the output directory: core, inference, extension and raw. Notice that the Pipeline module runs all required LHD modules sequentially for the final datasets generation.

###Core

The dataset contains types of all DBpedia resources represented by a DBpedia ontology type. If any textual match of a DBpedia resource is found in the DBpedia ontology, this DBpedia resource will be mapped to a DBpedia ontology type. This dataset contains most accurate and specific ontology types. For example:

    <http://dbpedia.org/resource/Country> -> <http://dbpedia.org/ontology/Country>
    
    # there is some textual match for the Country resource in the DBpedia ontology. It is mapped! 
    <http://dbpedia.org/resource/Chile> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Country> .

###Inference (STI algorithm)

In this dataset, all hypernyms which have a form as a dbpedia resource are assigned to a DBpedia ontology type by the STI algorithm. This dataset is not so accurate and specific like the Core dataset, but it is larger because it tries to map all resources with some extracted hypernym to DBpedia ontology types. For example:

    <http://dbpedia.org/resource/Republic> -> <http://dbpedia.org/ontology/Country>
    
    # Inferred DBpedia ontology type
    <http://dbpedia.org/resource/Germany> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Place> .
    
    <http://dbpedia.org/resource/Chile> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Country> .
    
    
###Extension

Resources have assigned a hypernym which was extracted from the HypernymExtractor module and mapped to some DBpedia resource.

    <http://dbpedia.org/resource/Angela_Merkel> <http://purl.org/linguistics/gold/hypernym> <http://dbpedia.org/resource/Politician> .

###Raw

This dataset contains hypernyms in the plain-text form. Example:

    <http://de.dbpedia.org/resource/Angela_Salem> <http://purl.org/linguistics/gold/hypernym> "Fu\u00DFballspielerin"@de .
    <http://de.dbpedia.org/resource/Lee_Hsin-han> <http://purl.org/linguistics/gold/hypernym> "Tennisspieler"@de .

