package eu.clarin.sru.fcs.aggregator.registry;

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
 * Represents collection of languages.
 * 
 * @author Yana Panchenko
 */
public class Languages {
	private static Languages instance = null;
    
    private Map<String,Language> code2Lang = new HashMap<String,Language>();
    
    public static final String LANGUAGES_FILE_PATH = "/lang/ISO-639-2_utf-8.txt";
    public static final String LANGUAGES_FILE_ENCODING = "UTF-8";
    public static final String ANY_LANGUAGE_NAME = "anylang";
    
	private Languages() {
        loadMapping();
	}

	public static Languages getInstance() {
		if (instance == null) {
			instance = new Languages();
		}
		return instance;
	}
    
    /**
     * Gets language by its ISO 639 language code.
     * 
     * @param code ISO 639/1, 639/2T or 639/2B language code
     * @return language
     */
    public Language langForCode(String code) {
        return this.code2Lang.get(code);
    }
    
    /**
     * Gets language name by the ISO 639 language code.
     * 
     * @param code ISO 639/1, 639/2T or 639/2B language code
     * @return language name in English
     */
    public String nameForCode(String code) {
        if (this.code2Lang.containsKey(code)) {
            return this.code2Lang.get(code).getNameEn();
        }
        return code;
    }

    /**
     * Gets all known to it ISO 639/1, 639/2T and 639/2B language codes.
     * 
     * @return language codes
     */
    public Set<String> getCodes() {
        return this.code2Lang.keySet();
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
                    Language lang = new Language(alpha2, alpha3t, alpha3b, enName);
                    if (!alpha3b.isEmpty()) {
                        this.code2Lang.put(alpha3b, lang);
                    }
                    if (!alpha3t.isEmpty()) {
                        this.code2Lang.put(alpha3t, lang);
                    }
                    if (!alpha2.isEmpty()) {
                        this.code2Lang.put(alpha2, lang);
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
