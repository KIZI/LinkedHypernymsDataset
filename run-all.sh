#!/bin/sh

# New LHD version downloading + installing
git pull
mvn clean
mvn install

# Downloading of required DBpedia datasets
(cd Downloader; mvn scala:run -DaddArgs=../global.properties)

# DBpedia resources indexing (short abstracts + types)
(cd HypernymExtractor; mvn scala:run -Dlauncher=indexer -DaddArgs=module.properties)

# Removing of old output LHD files
rm -R data/output/*

# Hypernym Extraction (short abstract analysis -> tree-tagger -> Hypernym extraction by JAPE Grammar -> map hypernyms to DBpedia resources)
# One Thread
#(cd HypernymExtractor; mvn scala:run -Dlauncher=runner -DaddArgs=module.properties)
# Multi Thread
(cd MapReduce; mvn scala:run -Dlauncher=starter -DaddArgs=10000)

# Mapping of DBpedia resources to DBpedia ontology by text-matching; Resources cleaning
(cd LHDOntologyCleanup; mvn scala:run -DaddArgs=module.properties)

# Mapping of all DBpedia resource to DBpedia ontology by statistical type inference
# Creating of draft files LHDv1.0, LHDv2.0, plainHyp
(cd LHDTypeInferrer; mvn scala:run -DaddArgs=module.properties)

echo "++ LHD EXTRACTION PROCESS WAS SUCCESSFUL ++"