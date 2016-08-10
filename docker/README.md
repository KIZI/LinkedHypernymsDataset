# LinkedHypernymDataset in Docker

A docker image of the current LHD extraction framework with all required dependencies (Gate, TreeTagger, etc.) can be created by a following docker build script:

```docker build -t lhd:latest https://github.com/KIZI/LinkedHypernymsDataset.git#:docker```

After the docker image has been successfully built, you can use the image for running of an extraction process by this command:

```docker run --name lhd -d --env-file <path-to-env-vars-file> lhd <language(en|de|nl)> <dbpedia-version>```

* __path-to-env-vars-file__ is a path to a file which contains required environment variables. The variables should contain URL links to DBpedia datasets that are required at the input. Supported dataset formats are N-Triples (.nt) and Turtle (.ttl) which may be compressed by GZIP (.gz) or BZIP2 (.bz2).
  * LHD_ONTOLOGY_URL=http://path/to/dbpedia.owl(.bz2|.gz)?
  * LHD_INSTANCE_TYPES_URL=http://path/to/lang-instance-types.(ttl|nt)(.bz2|.gz)?
  * LHD_INSTANCE_TYPES_EN_URL=http://path/to/en-instance-types.(ttl|nt)(.bz2|.gz)?
  * LHD_INSTANCE_TYPES_TRANSITIVE_URL=http://path/to/lang-instance-types-transitive.(ttl|nt)(.bz2|.gz)?
  * LHD_INSTANCE_TYPES_TRANSITIVE_EN_URL=http://path/to/en-instance-types-transitive.(ttl|nt)(.bz2|.gz)?
  * LHD_INTERLANGUAGE_LINKS_EN_URL=http://path/to/en-interlanguage-links.(ttl|nt)(.bz2|.gz)?
  * LHD_DISAMBIGUATIONS_URL=http://path/to/lang-disambiguations.(ttl|nt)(.bz2|.gz)?
  * LHD_SHORT_ABSTRACTS_URL=http://path/to/lang-short-abstracts.(ttl|nt)(.bz2|.gz)?
* __language__ is a selected language for which linked hypernym datasets are going to be created. Supported languages are English (en), German (de) and Dutch (nl).
* __dbpedia-version__ is a dbpedia version which is associated with the actual LHD extraction process.

Example:

```docker run --name lhd -d --env-file examples/datasets_de lhd en 2016-04```

After running an LHD docker container from the image, the extraction process is being in progress. It can take several hours or days - it depends on the number of available cores and the size of input datasets. After the completion of the extraction process, the docker container will contain all linked hypernym datasets for the selected language that are placed in the data/output directory. It only remains to copy datasets from the container to your local disk for other purposes:

