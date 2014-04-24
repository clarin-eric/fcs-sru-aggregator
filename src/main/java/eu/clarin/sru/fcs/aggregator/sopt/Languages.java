package eu.clarin.sru.fcs.aggregator.sopt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Yana Panchenko
 */
public class Languages {
    
    private Map<String,String> code2Name = new HashMap<String,String>();
    private Map<String,String> code22Code = new HashMap<String,String>();
    public static final String LANGUAGES_FILE_PATH = "/lang/ISO-639-2_utf-8.txt";
    public static final String LANGUAGES_FILE_ENCODING = "UTF-8";
    public static final String ANY_LANGUAGE_NAME = "anylang";
    
    public Languages() {
        loadMapping();
    }
    
    public String nameForCode(String code) {
        return this.code2Name.get(code);
    }
    
    public String code2ForCode(String code) {
        return this.code22Code.get(code);
    }
    
    public Set<String> getCodes() {
        return this.code2Name.keySet();
    }

    private void loadMapping() {
        InputStream is = Languages.class.getResourceAsStream(LANGUAGES_FILE_PATH);
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(is, LANGUAGES_FILE_ENCODING));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() > 0) {
                    String[] splitted = line.split("\\|");
                    String alpha3b = splitted[0];
                    String alpha3t = splitted[1];
                    String alpha2 = splitted[2];
                    String enName = splitted[3];
                    if (!alpha3b.isEmpty()) {
                        this.code2Name.put(alpha3b, enName);
                        this.code22Code.put(alpha3b, alpha2);
                    }
                    if (!alpha3t.isEmpty()) {
                        this.code2Name.put(alpha3t, enName);
                        this.code22Code.put(alpha3t, alpha2);
                    }
                    if (!alpha2.isEmpty()) {
                        this.code2Name.put(alpha2, enName);
                        this.code22Code.put(alpha2, alpha2);
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Languages.class.getName()).log(Level.SEVERE, "Initialization of languages code to name mapping falied.", ex);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(Languages.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
