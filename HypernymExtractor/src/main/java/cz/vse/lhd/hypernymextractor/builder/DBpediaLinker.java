package cz.vse.lhd.hypernymextractor.builder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.*;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.FailureMode;
import net.spy.memcached.MemcachedClient;

/**
 *
 * @author Milan Dojčinovski
 * <dojcinovski.milan (at) gmail.com>
 * Twitter:
 * @m1ci www: http://dojchinovski.mk
 */
public class DBpediaLinker {

    private final String APIBase;
    private final String lang;
    private MemcachedClient memClient;

    public DBpediaLinker(String _APIBase, String _lang, String address, int port) {
        try {
            memClient = new MemcachedClient(new ConnectionFactoryBuilder().setDaemon(true).setFailureMode(FailureMode.Retry).build(), Arrays.asList(new InetSocketAddress(address, port)));
        } catch (IOException e) {
            Logger.getGlobal().log(Level.SEVERE, null, "Memcached init failed");
        }
        APIBase = _APIBase;
        lang = _lang;
    }

    public void close() {
        memClient.shutdown();
    }

    private String getFromCache(String key) {
        if (memClient == null) {
            return null;
        }
        if (key.length() > 200) {
            key = MD5(key);
        }

        Object o = memClient.get(key);
        if (o == null) {
            return null;
        }
        //Logger.getGlobal().log(Level.INFO, "retrieved from cache: {0}", (String) o);
        return (String) o;
    }

    private String MD5(String md5) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
        }
        return null;
    }

    private void saveToCache(String key, String value) {
        if (memClient == null) {
            return;
        }
        if (key.length() > 200) {
            key = MD5(key);
        }
        memClient.set(key, 604800, value);
    }

    public String getLink(String input) {
        if (input == null) {
            return null;
        } else {
            String cached = getFromCache(input);
            if (cached != null) {
                return cached;
            }

            input = input.replaceAll(" ", "_");
            try {
                input = URLEncoder.encode(input, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                Logger.getGlobal().log(Level.SEVERE, null, ex);
            }

            URL url;
            StringBuilder buffer = new StringBuilder();
            Pattern articleTitle = null;
            for (int i = 0; i < 10; i++) {
                try {
                    //System.out.println("Fetching url for linking: " + wikiBase + spanPath+"&srlimit=" + 5 + "&srsearch=" + input);
                    long start = System.currentTimeMillis();

                    if (APIBase.contains("search")) {
                        //e.g. http://ner.vse.cz:8125/search/de_wikimirror/iron%20lady?limit=1
                        url = new URL(APIBase + input + "?limit=" + 1);
                        articleTitle = Pattern.compile("^\\d.*\\s(\\S+)$", Pattern.MULTILINE);
                    } else {
                    //api search with xml formatted results
                        // e.g. http://ner.vse.cz/wiki/api.php?action=query&format=xml&list=search&srwhat=text&srsearch=film

                        //(!) srwhat=nearmatch seems to be giving better results than srwhat=text, 
                        //e.g. player is mapped to Player with nearmatch, but to John Player & Sons with text
                        url = new URL(APIBase + "api.php?action=query&format=xml&list=search&srwhat=nearmatch" + "&srlimit=" + 1 + "&srsearch=" + input);
                        //articleTitle_text = Pattern.compile("title=\"(.*?)\"", Pattern.DOTALL);  
                        articleTitle = Pattern.compile(" title=\"([^\"]+)\" ", Pattern.MULTILINE);

                    }
                    //Logger.getGlobal().log(Level.INFO, url.toString());
                    URLConnection connection = url.openConnection();
                    InputStream is = connection.getInputStream();
                    Reader isr = new InputStreamReader(is, "UTF-8");
                    Reader in = new BufferedReader(isr);
                    int ch;

                    while ((ch = in.read()) > -1) {
                        buffer.append((char) ch);
                    }
                    in.close();
                    long elapsedTime = System.currentTimeMillis() - start;
                    break;
                    //System.out.println("Document for linking fetched in: " + elapsedTime/1000F + "s ");                
                } catch (ConnectException e) {
                    Logger.getGlobal().log(Level.INFO, "{0}. Retry #{1}", new Object[]{e.getMessage(), i});
                    try {
                        Thread.sleep((long) (Math.pow(2, i) * 1000));
                    } catch (InterruptedException ex) {
                    }
                } catch (IOException e) {
                    Logger.getGlobal().log(Level.SEVERE, null, e);
                    return null;
                }
            }

            if (articleTitle == null) {
                return null;
            }

            String searchResult = buffer.toString();

            Matcher titleMatcher = articleTitle.matcher(searchResult);

            String result = null;

            while (titleMatcher.find()) {
                String title = titleMatcher.group(1);
                //wikiEntries.add(title);
                //System.out.println(title);
                if (lang.equals("en")) {
                    result = "http://dbpedia.org/resource/" + titleMatcher.group(1).replaceAll(" ", "_");
                } else {
                    result = "http://" + lang + ".dbpedia.org/resource/" + titleMatcher.group(1).replaceAll(" ", "_");
                }

            }
            if (result != null) {
                saveToCache(input, result);
                return result;
            } else {
                return null;
            }
        }
    }
}
