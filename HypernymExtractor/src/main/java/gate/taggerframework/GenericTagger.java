package gate.taggerframework;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.ProcessingResource;
import gate.Resource;
import gate.Utils;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.creole.Transducer;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.util.BomStrippingInputStreamReader;
import gate.util.Files;
import gate.util.OffsetComparator;
import gate.util.ProcessManager;
import gate.util.Strings;
import java.io.BufferedInputStream;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * This processing resource is designed to allow the easy use of a number of
 * external (non Java) taggers within GATE. A number of assumptions have been
 * made about the external taggers in order to provide this framework, but any
 * tagger that expects one annotation type per line and outputs one annotation
 * type per line (input and output do not have to be the same) should be
 * compatible with this PR.
 *
 * @author Mark A. Greenwood
 * @author Rene Witte
 */
@CreoleResource(comment = "The Generic Tagger is Generic!",
        helpURL = "http://gate.ac.uk/userguide/sec:parsers:taggerframework")
public class GenericTagger extends AbstractLanguageAnalyser implements
        ProcessingResource {

    private static final long serialVersionUID = 1065568768231638007L;
    // Pattern to match ${...} placeholder sequences in the inputTemplate
    protected static final Pattern PLACEHOLDER_PATTERN = Pattern
            .compile("\\$\\{([^\\}]+)\\}");
    // TODO Think about moving this to a runtime parameter
    public static final String STRING_FEATURE_NAME = "string";
    // The transducers that will do the pre and post processing
    private Transducer preProcess, postProcess;
    // The URLs of the JAPE grammars for pre and post processing
    private URL preProcessURL, postProcessURL;
    // The annotations sets used for input and output
    private String inputASName, outputASName;
    // The character encoding the tagger expects and a regex to process
    // the output
    private String encoding, regex;
    // the path to the tagger binary
    private URL taggerBinary, taggerDir;
    // flags to pass to the tagger
    private List<String> taggerFlags;
    // should we...
    // fail if mapping between charsets fails
    // display debug information
    // update or replace existing output annotations
    private Boolean failOnUnmappableCharacter, debug, updateAnnotations, failOnMissingInputAnnotations;
    // The type of the input and output annotations
    private String inputAnnotationType, outputAnnotationType;
    // a comma separated list of feature names mapped to regex capturing
    // groups
    private FeatureMap featureMapping;
    // A template string defining how to map annotation features to the
    // line of text that will be passed to the tagger.
    protected String inputTemplate;
    // A copy of the inputTemplate with Java escape sequences like \t
    // unescaped
    protected String inputTemplateUnescaped;
    //a util class for dealing with external processes, i.e. the tagger
    private ProcessManager processManager = new ProcessManager();
    protected Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * This method initialises the tagger. This involves loading the pre and
     * post processing JAPE grammars as well as a few sanity checks.
     *
     * @throws ResourceInstantiationException if an error occurs while
     * initialising the PR
     */
    @Override
    public Resource init() throws ResourceInstantiationException {

        // Not sure if this is definitely needed but it makes sense that on
        // certain platforms the external call may well fail if the paths
        // contain spaces as the shell could interpret the space as a change
        // in command line argument rather than part of a path.
        String tmpDir = System.getProperty("java.io.tmpdir");
        if (tmpDir == null || tmpDir.indexOf(' ') >= 0) {
            throw new ResourceInstantiationException(
                    "The tagger requires your temporary directory to be set to a value "
                    + "that does not contain spaces.  Please set java.io.tmpdir to a "
                    + "suitable value.");
        }

        // Create a feature map that we can use to load the two JAPE
        // transducers making sure they are hidden (i.e. don't appear in the
        // list of loaded PRs)
        FeatureMap hidden = Factory.newFeatureMap();
        Gate.setHiddenAttribute(hidden, true);
        FeatureMap params = Factory.newFeatureMap();

        if (preProcessURL != null) {
            // if there is a pre-processing JAPE grammar then load it
            params.put("grammarURL", preProcessURL);

            if (preProcess == null) {
                preProcess = (Transducer) Factory.createResource(
                        "gate.creole.Transducer", params, hidden);
            } else {
                preProcess.setParameterValues(params);
                preProcess.reInit();
            }
        }

        if (postProcessURL != null) {
            // if there is a post-processing grammar then load it
            params.put("grammarURL", postProcessURL);

            if (postProcess == null) {
                postProcess = (Transducer) Factory.createResource(
                        "gate.creole.Transducer", params, hidden);
            } else {
                postProcess.setParameterValues(params);
                postProcess.reInit();
            }
        }

        return this;
    }

    @Override
    public void cleanup() {
        if (preProcess != null) {
            Factory.deleteResource(preProcess);
        }
        if (postProcess != null) {
            Factory.deleteResource(postProcess);
        }
    }

    /**
     * This method does all the work by calling the protected methods in the
     * right order so that an input file is written, a command line is built,
     * the tagger is run and then finally the output of the tagger is added as
     * annotations onto the GATE document being processed.
     *
     * @throws ExecutionException if an error occurs during any stage of running
     * the tagger
     */
    @Override
    public void execute() throws ExecutionException {
        // do some sanity checking of the runtime parameters before we start
        // doing any work

        if (document == null) {
            throw new ExecutionException("No document to process!");
        }

        if (taggerBinary == null) {
            throw new ExecutionException(
                    "Cannot proceed unless a tagger executable is specified.");
        }

        if (encoding == null) {
            throw new ExecutionException("No encoding specified");
        }

        if (regex == null || regex.trim().equals("")) {
            throw new ExecutionException(
                    "A regular exception for processing the tagger output must be provided");
        }

        if (!featureMapping.containsKey("string")) {
            throw new ExecutionException(
                    "The feature mapping must include an entry for 'string' in order to map between the tagger and the GATE document/annotations");
        }

        if (preProcess != null) {
            // if there is a pre-processing work to be done then run the
            // supplied JAPE grammar
            preProcess.setInputASName(inputASName);
            preProcess.setOutputASName(inputASName);
            preProcess.setDocument(document);

            try {
                preProcess.execute();
            } finally {
                preProcess.setDocument(null);
            }
        }
        
        // get current text from GATE for the tagger
        File textfile = getCurrentText();
        if (textfile == null) {
            /* This handles the null return value from getCurrentText() when
             * there are no input annotations in the document and
             * parameter failOnMissingInputAnnotations is false.       */
            return;
        }

        // build the command line for running the tagger
        String[] taggerCmd = buildCommandLine(textfile);
        
        // run the tagger and put the output back into GATE
        readOutput(runTagger(taggerCmd));

        if (postProcess != null) {
            // /if there is post-processing work to be done then run the
            // supplied JAPE grammar
            postProcess.setInputASName(outputASName);
            postProcess.setOutputASName(outputASName);
            postProcess.setDocument(document);

            try {
                postProcess.execute();
            } finally {
                postProcess.setDocument(null);
            }
        }

        // delete the temporary text file
        if (!debug) {
            if (!textfile.delete()) {
                textfile.deleteOnExit();
            }
        }
    }

    /**
     * This method constructs an array of Strings which will be used as the
     * command line for executing the external tagger through a call to
     * Runtime.exec(). This uses the tagger binary and flags to build the
     * command line. If the system property
     * <code>shell.path</code> has been set then the command line will be built
     * so that the tagger is run by the provided shell. This is useful on
     * Windows where you will usually need to run the tagger under Cygwin or the
     * Command Prompt.
     *
     * @param textfile the file containing the input to the tagger
     * @return a String array containing the correctly assembled command line
     * @throws ExecutionException if an error occurs whilst building the command
     * line
     */
    protected String[] buildCommandLine(File textfile) throws ExecutionException {
        // check that the file exists
        File scriptfile = Files.fileFromURL(taggerBinary);
        if (scriptfile.exists() == false) {
            throw new ExecutionException("Script " + scriptfile.getAbsolutePath()
                    + " does not exist");
        }

        // a pointer to where to stuff the flags
        int index = 0;

        // the array we are buiding
        String[] taggerCmd;

        // the bath to a shell under which to run the script
        String shPath = System.getProperty("shell.path");

        if (shPath != null) {
            // if there is a shell then use that as the first command line
            // argument
            taggerCmd = new String[3 + taggerFlags.size()];
            taggerCmd[0] = shPath;
            index = 1;
        } else {
            // there is no shell so we only need an array long enough to hold
            // the binary, flags and file
            taggerCmd = new String[2 + taggerFlags.size()];
        }

        // get an array from the list of flags
        String[] flags = taggerFlags.toArray(new String[0]);

        // copy the flags into the array we are building
        System.arraycopy(flags, 0, taggerCmd, index + 1, flags.length);

        // add the binary and input file to the command line
        taggerCmd[index] = scriptfile.getAbsolutePath();
        taggerCmd[taggerCmd.length - 1] = textfile.getAbsolutePath();

        if (debug) {
            // if we are doing debug work then echo the command line
            StringBuilder sanityCheck = new StringBuilder();
            for (String s : taggerCmd) {
                sanityCheck.append(" ").append(s);
            }
            System.out.println(sanityCheck.toString());
        }

        // return the fully constructed command line
        return taggerCmd;
    }

    /**
     * This method copies specific annotations from the current GATE document
     * into a file that can be read by the tagger.
     *
     * @return a File object which contains the input to the tagger
     * @throws ExecutionException if an error occurs while building the tagger
     * input file
     */
    protected File getCurrentText() throws ExecutionException {
        // the file we are going to write
        File gateTextFile = null;

        try {
            // create an empty temp file so we don't overwrite any existing
            // files
            gateTextFile = File.createTempFile("tagger", ".txt");

            // get the character set we should be using for encoding the file
            Charset charset = Charset.forName(encoding);

            // depending on the failOnUnmappableCharacter parameter, we either
            // make the output stream writer fail or replace the unmappable
            // character with '?'
            CharsetEncoder charsetEncoder = charset.newEncoder()
                    .onUnmappableCharacter(
                    failOnUnmappableCharacter
                    ? CodingErrorAction.REPORT
                    : CodingErrorAction.REPLACE);

            // Get a stream we can write to that handles the encoding etc.
            FileOutputStream fos = new FileOutputStream(gateTextFile);
            OutputStreamWriter osw = new OutputStreamWriter(fos, charsetEncoder);
            BufferedWriter bw = new BufferedWriter(osw);

            // get the input annotation set that we should be using
            AnnotationSet annotSet = (inputASName == null || inputASName.trim()
                    .equals("")) ? document.getAnnotations() : document
                    .getAnnotations(inputASName);

            // filter the set so we just get the input annotations we need for
            // the tagger
            annotSet = annotSet.get(inputAnnotationType);
            if (annotSet == null || annotSet.size() == 0) {
                if (failOnMissingInputAnnotations) {
                    // if there are no input annotations then we can't do anything
                    // so throw an exception
                    throw new ExecutionException("No " + inputAnnotationType
                            + " found in the document.");
                } else {
                    Utils.logOnce(logger, Level.INFO, "GenericTagger: no sentence or token annotations in input document - see debug log for details.");
                    logger.debug("No input annotations in document " + document.getName());
                    return null;
                }
            }

            // sort tokens according to their offsets
            List<Annotation> inputAnnotations = new ArrayList<Annotation>(annotSet);
            Collections.sort(inputAnnotations, new OffsetComparator());

            // and now start writing them in a file
            for (int i = 0; i < inputAnnotations.size(); i++) {
                // write the string to the file
                bw.write(taggerInputFor(inputAnnotations.get(i)));

                // if there are more annotations to process then write a blank
                // line as well
                if (i + 1 < inputAnnotations.size()) {
                    bw.newLine();
                }
            }

            // we have finished writing the file so close the streams
            bw.close();
        } catch (CharacterCodingException cce) {
            throw (ExecutionException) new ExecutionException(
                    "Document contains a character that cannot be represented "
                    + "in " + encoding).initCause(cce);
        } catch (IOException ioe) {
            throw (ExecutionException) new ExecutionException(
                    "Error creating temporary file for tagger").initCause(ioe);
        }
        return (gateTextFile);
    }

    /**
     * Returns the text that will be sent to the tagger for a given annotation.
     * By default this is simply the value of the "string" feature from the
     * annotation. If this assumption does not match the tagger you wish to use
     * then you will need to create a subclass and override this method.
     *
     * @param ann the annotation
     * @return the string to be passed to the tagger for this annotation
     */
    protected String taggerInputFor(Annotation ann) throws ExecutionException {
        FeatureMap features = ann.getFeatures();
        StringBuffer buf = new StringBuffer();
        Matcher mat = PLACEHOLDER_PATTERN.matcher(inputTemplateUnescaped);
        // keep track of whether we have made any substitutions for this
        // annotation. If we haven't substituted in *any* feature values
        // for a given annotation then something is wrong...
        boolean substitutionMade = false;
        while (mat.find()) {
            String key = mat.group(1);
            if (features.containsKey(key)) {
                substitutionMade = true;
                mat.appendReplacement(buf,
                        Matcher.quoteReplacement(String.valueOf(features.get(key))));
            } else {
                // use an empty string if the annotation doesn't have the
                // requested
                // feature
                mat.appendReplacement(buf, "");
            }
        }

        if (substitutionMade) {
            mat.appendTail(buf);
            return buf.toString();
        } else {
            throw new ExecutionException(
                    "None of the specified input features were found in "
                    + "annotation " + ann);
        }
    }

    /**
     * This method is responsible for executing the external tagger. If a
     * problem is going to occur this is likely to be the place!
     *
     * @param cmdline the command line we want to execute
     * @return an InputStream from which the output of the tagger can be read
     * @throws ExecutionException if an error occurs executing the tagger
     */
    protected InputStream runTagger(String[] cmdline) throws ExecutionException {

        ByteArrayOutputStream baout = new ByteArrayOutputStream();

        try {
            int returnCode;

            if (taggerDir == null) {
                returnCode = processManager.runProcess(cmdline, baout, (debug ? System.err : null));
            } else {
                returnCode = processManager.runProcess(cmdline, Files.fileFromURL(taggerDir), baout, (debug ? System.err : null));
            }

            if (debug) {
                System.err.println("Return Code From Tagger: " + returnCode);
            }

            return new ByteArrayInputStream(baout.toByteArray());
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
    }

    /**
     * This method reads the output from the tagger adding the information back
     * into the GATE document. If the tagger doesn't produce one line per output
     * type then you will need to override this to do something different.
     *
     * @param in the InputStream frmo which the method will read the output from
     * the tagger
     * @throws ExecutionException if an error occurs while handling the output
     * from the tagger
     */
    protected void readOutput(InputStream in) throws ExecutionException {

        // get the right output annotation set based on the runtime
        // parameter settings
        AnnotationSet annotSet = (inputASName == null || inputASName.trim()
                .length() == 0) ? document.getAnnotations() : document
                .getAnnotations(inputASName);

        // filter so we only keep the pre-existing output annotations
        annotSet = annotSet.get(inputAnnotationType);

        // sort the pre-existing output annotations
        List<Annotation> inputAnnotations = new ArrayList<Annotation>(annotSet);
        Collections.sort(inputAnnotations, new OffsetComparator());

        // get the first input annotation which we want to sync with
        Annotation currentInput = inputAnnotations.remove(0);

        // get the output annotation set based on the runtime parameter
        // settings
        AnnotationSet aSet = (outputASName == null || outputASName.trim().length() == 0)
                ? document.getAnnotations()
                : document.getAnnotations(outputASName);

        // get a decoder using the charset
        Charset charset = Charset.forName(encoding);
        //CharsetDecoder charsetDecoder = charset.newDecoder();

        try {
            // get a reader over the output from the tagger remembering to
            // handle the encoding
            BufferedReader input = new BufferedReader(new InputStreamReader(in, encoding));
                    
            // compile the regular expression so that we can interpret the
            // output
            Pattern resultPattern = Pattern.compile(regex);

            // get the outputAnnotations that occur from the current location
            // onwards as we don't want to look at annotations we have already
            // dealt with
            List<Annotation> outputAnnotations = new ArrayList<Annotation>(aSet.get(
                    outputAnnotationType, currentInput.getStartNode().getOffset(),
                    currentInput.getEndNode().getOffset()));

            // if we are not updatind existing annotations then remove them
            // all else make sure they are sorted by offset
            if (!updateAnnotations) {
                aSet.removeAll(outputAnnotations);
            } else {
                Collections.sort(outputAnnotations, new OffsetComparator());
            }

            String line;
            int currentPosition = 0;

            while ((line = input.readLine()) != null) {
                // for each line in the output...

                // if we are debugging then dump the output line
                if (debug) {
                    System.out.println(new String(charset.encode(line).array(), encoding));
                }

                // get the matcher for the line from the regex
                Matcher m = resultPattern.matcher(line);

                if (m.matches()) {
                    // if the line matches then it contains data we need to
                    // process

                    // create an empty feature map to store the data
                    FeatureMap features = Factory.newFeatureMap();

                    for (Map.Entry<Object, Object> kv : featureMapping.entrySet()) {
                        // for each feature in the provided mapping, get the
                        // associated capture group from the matcher and store the
                        // info in the feature map
                        int groupNumber = Integer.parseInt(String.valueOf(kv.getValue()));
                        // ignore mapping if there isn't a match for that group
                        if (m.start(groupNumber) >= 0) {
                            features.put(kv.getKey(), m.group(groupNumber));
                        }
                    }

                    while (updateAnnotations && outputAnnotations.size() == 0) {
                        // if we are updating annotations but there is nothing to
                        // update then...

                        if (inputAnnotations.size() == 0) {
                            // if there are no new annotations then something has gone
                            // badly wrong and we are out of sync so throw an
                            // exception
                            throw new Exception("no remaining annotations of type "
                                    + outputAnnotationType + " to update");
                        }

                        // move onto the next input annotation
                        currentInput = inputAnnotations.remove(0);

                        // find the matching output annotations and then sort them
                        outputAnnotations.addAll(aSet.get(outputAnnotationType,
                                currentInput.getStartNode().getOffset(), currentInput
                                .getEndNode().getOffset()));
                        Collections.sort(outputAnnotations, new OffsetComparator());
                    }

                    // get the next annotation if there is any
                    Annotation next = (outputAnnotations.size() == 0
                            ? null
                            : outputAnnotations.remove(0));

                    if (next != null && updateAnnotations) {
                        // if there is an annotation and we are updating then...

                        // get an encoded version of the input annotations string
                        // feature
                        String encoded = new String(charset.encode(
                                (String) next.getFeatures().get(STRING_FEATURE_NAME))
                                .array(), encoding).trim();

                        // if the encoded version isn't the same as the output from
                        // the tagger we have lost sync so throw an exception
                        if (!encoded.equals(features.get(STRING_FEATURE_NAME))) {
                            throw new Exception("annotations are out of sync: " + encoded
                                    + " != " + features.get(STRING_FEATURE_NAME));
                        }

                        // remove the feature name so we use the existing one rather
                        // then the encoded version
                        features.remove(STRING_FEATURE_NAME);

                        // add the features from the tagger to the existing
                        // annotation
                        next.getFeatures().putAll(features);
                    } else {
                        // if we aren't updating then...

                        // get a section of the document where we think the
                        // annotation will be and encode it so it matches the output
                        // from the tagger
                        String encodedInput = new String(charset.encode(
                                document.getContent()
                                .getContent(
                                currentInput.getStartNode().getOffset(),
                                currentInput.getEndNode().getOffset())
                                .toString()).array(), encoding).trim();

                        while ((currentPosition = encodedInput.indexOf(
                                (String) features.get("string"), currentPosition)) == -1) {
                            // whilst we can't find the current item from the tagger
                            // in the input...

                            // if there are no more input annotations then we are out
                            // of sync so panic and throw and exception
                            if (inputAnnotations.size() == 0) {
                                throw new Exception("no remaning annotations of type "
                                        + inputAnnotationType + " to add within");
                            }

                            // move onto the next input annotation
                            currentInput = inputAnnotations.remove(0);

                            // remove all the output annotations spanning the current
                            // input annotation
                            aSet.removeAll(aSet.get(outputAnnotationType, currentInput
                                    .getStartNode().getOffset(), currentInput.getEndNode()
                                    .getOffset()));

                            // reset the position inside the current input annotation
                            // to 0
                            currentPosition = 0;

                            // and finally get the encoded document section matching
                            // the input annotation again
                            encodedInput = new String(charset.encode(
                                    document.getContent()
                                    .getContent(
                                    currentInput.getStartNode().getOffset(),
                                    currentInput.getEndNode().getOffset())
                                    .toString()).array(), encoding).trim();
                        }

                        // if we get to here then we have found the right place to
                        // add an annotation, yeah!

                        // work out the start and end position of the new annotation
                        Long start = currentPosition
                                + currentInput.getStartNode().getOffset();
                        Long end = start
                                + ((String) features.get(STRING_FEATURE_NAME)).length();

                        // add the output annotation with the features
                        aSet.add(start, end, outputAnnotationType, features);

                        // update the current position within the input annotation
                        // to the end of the annotation we just addeed
                        currentPosition = (int) (end - currentInput.getStartNode()
                                .getOffset());
                    }
                } else if (debug) {
                    System.err.println("Line didn't match input pattern!");
                }
            }
        } catch (Exception err) {
//            err.printStackTrace();
//            throw (ExecutionException) new ExecutionException(
//                    "Error occurred running tagger").initCause(err);
        }
    }

    public String getInputTemplate() {
        return inputTemplate;
    }

    @RunTime
    @CreoleParameter(defaultValue = "${string}", comment = "Template used to build the line of input "
            + "to the tagger from the features of a single "
            + "annotation.  Should include ${feature} placeholders "
            + "which will be replaced by the value of the "
            + "corresponding feature from the annotation.")
    public void setInputTemplate(String inputTemplate) {
        this.inputTemplate = inputTemplate;
        this.inputTemplateUnescaped = Strings.unescape(inputTemplate);
    }

    public FeatureMap getFeatureMapping() {
        return featureMapping;
    }

    @RunTime
    @CreoleParameter(defaultValue = "string=1;category=2;lemma=3", comment = "mapping from feature names to matching groups, this must include a mapping for 'string'")
    public void setFeatureMapping(FeatureMap featureMapping) {
        this.featureMapping = featureMapping;
    }

    public URL getTaggerBinary() {
        return taggerBinary;
    }

    @RunTime
    @CreoleParameter(comment = "Name of the tagger command file")
    public void setTaggerBinary(URL taggerBinary) {
        this.taggerBinary = taggerBinary;
    }

    public URL getTaggerDir() {
        return taggerDir;
    }

    @Optional
    @RunTime
    @CreoleParameter(comment = "directory in which to run the tagger")
    public void setTaggerDir(URL taggerDir) {
        this.taggerDir = taggerDir;
    }

    public List<String> getTaggerFlags() {
        return taggerFlags;
    }

    @RunTime
    @CreoleParameter(defaultValue = "", comment = "flags passed to tagger script")
    public void setTaggerFlags(List<String> taggerFlags) {
        this.taggerFlags = taggerFlags;
    }

    public Boolean getUpdateAnnotations() {
        return updateAnnotations;
    }

    @RunTime
    @CreoleParameter(defaultValue = "true", comment = "do you want to update annotations or add new ones?")
    public void setUpdateAnnotations(Boolean updateAnnotations) {
        this.updateAnnotations = updateAnnotations;
    }

    public String getRegex() {
        return regex;
    }

    @RunTime
    @CreoleParameter(defaultValue = "(.+)\t(.+)\t(.+)", comment = "regex to process tagger ouptut, this must contain capturing groups which can then be referenced in the feature mapping")
    public void setRegex(String regex) {
        this.regex = regex;
    }

    public String getInputAnnotationType() {
        return inputAnnotationType;
    }

    @RunTime
    @CreoleParameter(defaultValue = "Token", comment = "annotation used as input to tagger")
    public void setInputAnnotationType(String inputAnnotationType) {
        this.inputAnnotationType = inputAnnotationType;
    }

    public String getOutputAnnotationType() {
        return outputAnnotationType;
    }

    @RunTime
    @CreoleParameter(defaultValue = "Token", comment = "annotation output by tagger")
    public void setOutputAnnotationType(String outputAnnotationType) {
        this.outputAnnotationType = outputAnnotationType;
    }

    public Boolean getDebug() {
        return debug;
    }

    @RunTime
    @CreoleParameter(defaultValue = "false", comment = "turn on debugging options")
    public void setDebug(Boolean debug) {
        this.debug = debug;
    }

    public String getInputASName() {
        return inputASName;
    }

    @Optional
    @RunTime
    @CreoleParameter(comment = "annotation set in which annotations are created")
    public void setInputASName(String inputASName) {
        this.inputASName = inputASName;
    }

    public String getOutputASName() {
        return outputASName;
    }

    @Optional
    @RunTime
    @CreoleParameter(comment = "annotation set in which annotations are created")
    public void setOutputASName(String outputASName) {
        this.outputASName = outputASName;
    }

    public String getEncoding() {
        return encoding;
    }

    @RunTime
    @CreoleParameter(defaultValue = "ISO-8859-1", comment = "Character encoding for temporary files, must match "
            + "the encoding of your tagger data files")
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public Boolean getFailOnUnmappableCharacter() {
        return failOnUnmappableCharacter;
    }

    @RunTime
    @CreoleParameter(defaultValue = "true", comment = "Should the tagger fail if it encounters a character which "
            + "is not mappable into the specified encoding?")
    public void setFailOnUnmappableCharacter(Boolean failOnUnmappableCharacter) {
        this.failOnUnmappableCharacter = failOnUnmappableCharacter;
    }

    public URL getPreProcessURL() {
        return preProcessURL;
    }

    @Optional
    @CreoleParameter(comment = "JAPE grammar to use for pre-processing")
    public void setPreProcessURL(URL preProcessURL) {
        this.preProcessURL = preProcessURL;
    }

    public URL getPostProcessURL() {
        return postProcessURL;
    }

    @Optional
    @CreoleParameter(comment = "JAPE grammar to use for post-processing")
    public void setPostProcessURL(URL postProcessURL) {
        this.postProcessURL = postProcessURL;
    }

    @RunTime
    @Optional
    @CreoleParameter(
            comment = "Throw an exception when there are none of the required input annotations",
            defaultValue = "true")
    public void setFailOnMissingInputAnnotations(Boolean fail) {
        failOnMissingInputAnnotations = fail;
    }

    public Boolean getFailOnMissingInputAnnotations() {
        return failOnMissingInputAnnotations;
    }
}
