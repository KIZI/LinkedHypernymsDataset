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

# LHD Pipeline
(cd Pipeline; mvn scala:run -DaddArgs="$1|remove-all") || failure "LHD Generation Failed"

echo "++ LHD EXTRACTION PROCESS WAS SUCCESSFUL ++"