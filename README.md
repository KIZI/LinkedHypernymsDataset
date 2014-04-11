LinkedHypernymsDataset
======================

LinkedHypernymsDataset extraction framework makes RDF dataset consists of DBpedia resources (as subjects) and types of the DBpedia resources (as objects). The extraction framework returns two particular datasets LHD1.0 (types of resources are other resources or DBpedia ontology classes) and LHD2.0 (types of resources are DBpedia ontology classes only). The extraction process tries to find the hyperonymum for each DBpedia resource (HypernymExtractor module) which is transformed to another DBpedia resource (LHDOntologyCleanup module) and then is mapped to a DBpedia ontology class (LHDTypeInferrer module). Supported languages are English, German and Dutch.
