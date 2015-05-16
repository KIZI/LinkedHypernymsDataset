package cz.vse.lhd.lhdontologycleanup;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author tomas
 */
public class ManualMapping {

    private final HashMap<String, String> overrideMappings = new HashMap();
    private final HashSet<String> excludeTypes = new HashSet();

    public ManualMapping(String overridePath, String excludePath) throws IOException {
        if (overridePath != null) {
            readOverrideTypesFile(overridePath);
        }
        if (excludePath != null) {
            readExcludeTypesFile(excludePath);
        }
    }

    private void readOverrideTypesFile(String path) {
        FileInputStream fstream = null;
        try {
            fstream = new FileInputStream(path);
            // Get the object of DataInputStream
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String thisLine;
            while ((thisLine = br.readLine()) != null) {
                if (thisLine.startsWith("#")) {
                    continue;
                }
                //remove comments
                thisLine = thisLine.replaceAll("#.*", "");
                if (!thisLine.contains("dbpedia.org/resource")) {
                    continue;
                }

                int indexOfMapFromEnd = thisLine.indexOf(" ");
                String mapFrom = thisLine.substring(0, indexOfMapFromEnd).trim();
                String mapTo = thisLine.substring(indexOfMapFromEnd, thisLine.length()).trim();
                overrideMappings.put(mapFrom, mapTo);
            }
        } catch (IOException ex) {
        } finally {
            try {
                if (fstream != null) {
                    fstream.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(ManualMapping.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    public String getOverridingType(String type) {
        return overrideMappings.get(type);
    }

    public boolean isExcluded(String type) {
        return excludeTypes.contains(type);
    }

    private void readExcludeTypesFile(String path) {
        FileInputStream fstream = null;
        try {
            fstream = new FileInputStream(path);
            // Get the object of DataInputStream
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String thisLine;
            while ((thisLine = br.readLine()) != null) {
                if (thisLine.startsWith("#")) {
                    continue;
                }
                //remove comments
                thisLine = thisLine.replaceAll("#.*", "");
                if (!thisLine.contains("dbpedia.org/resource")) {
                    continue;
                }
                //to save memory only the concept name is stored, not full uri            
                String exclude = thisLine.trim();
                excludeTypes.add(exclude);
            }
        } catch (IOException ex) {
        } finally {
            try {
                if (fstream != null) {
                    fstream.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(ManualMapping.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }
}
