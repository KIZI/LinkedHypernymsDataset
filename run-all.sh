#!/bin/bash

function failure()
{
    echo "$@" >&2
    exit 1
}

if [ -z "$1" ]
  then
    echo "No argument passed. You need to specify the first argument as a path to your config file (e.g. application.conf)."
    exit 1
fi

cd Core
if [ ! -f $1 ]; then
    echo "The config file not found!"
    exit 1
fi
cd ..

# New LHD version downloading + installing
git pull
mvn clean
mvn install

# Downloading of required DBpedia datasets
(cd Downloader; mvn scala:run -DaddArgs=$1) || failure "Failed to download all datasets"

# DBpedia resources indexing (short abstracts + types)
(cd HypernymExtractor; mvn scala:run -Dlauncher=indexer -DaddArgs=$1) || failure "Failed to index datasets"

# Removing of old output LHD files
rm -R data/output/*

# Hypernym Extraction (short abstract analysis -> tree-tagger -> Hypernym extraction by JAPE Grammar -> map hypernyms to DBpedia resources)
# One Thread
#(cd HypernymExtractor; mvn scala:run -Dlauncher=runner -DaddArgs=$1)
# Multi Thread
(cd MapReduce; mvn scala:run -Dlauncher=starter -DaddArgs=10000) || failure "Failed to extract all hypernyms"

# Mapping of DBpedia resources to DBpedia ontology by text-matching; Resources cleaning
(cd LHDOntologyCleanup; mvn scala:run -DaddArgs=$1) || failure "Failed to clean hypernym datasets"

# Mapping of all DBpedia resource to DBpedia ontology by statistical type inference
# Creating of draft files LHDv1.0, LHDv2.0, plainHyp
(cd LHDTypeInferrer; mvn scala:run -DaddArgs=$1) || failure "Failed to infer types of hypernyms"

echo "++ LHD EXTRACTION PROCESS WAS SUCCESSFUL ++"