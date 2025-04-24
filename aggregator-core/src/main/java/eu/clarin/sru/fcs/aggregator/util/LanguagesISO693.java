package eu.clarin.sru.fcs.aggregator.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.LoggerFactory;

/**
 * Utility class to convert between various language codes.
 *
 * @author Yana Panchenko
 */
public class LanguagesISO693 {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(LanguagesISO693.class);

    public static final String LANGUAGES_FILE_PATH = "/lang/iso-639-3_20140320.tab";
    public static final String LANGUAGES_FILE_ENCODING = "UTF-8";

    private static LanguagesISO693 instance = null;

    public static class Language {

        // code_3 is ISO-639-3 (3 letters)
        public final String code_3;
        // code_1 is ISO-639-1 (2 letters)
        public final String code_1;
        // English language name
        public final String name;

        public Language(String code_3, String code_1, String name) {
            this.code_3 = code_3;
            this.code_1 = code_1;
            this.name = name;
        }
    }

    private Map<String, Language> codeToLang = new HashMap<String, Language>();
    private Map<String, Language> nameToLang = new HashMap<String, Language>();

    private LanguagesISO693() {
        InputStream is = LanguagesISO693.class.getResourceAsStream(LANGUAGES_FILE_PATH);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, LANGUAGES_FILE_ENCODING))) {
            br.readLine(); // ignore first line (header)
            String line;
            while ((line = br.readLine()) != null) {
                if (line.length() > 0) {
                    String[] toks = line.split("\\t");
                    if (toks.length != 7 && toks.length != 8) {
                        log.error("Line error in language codes file: '{}'", line);
                        continue;
                    }
                    String code_3 = toks[0].trim();
                    String code_1 = toks[3].trim().isEmpty() ? null : toks[3].trim();
                    if (code_1 != null && code_1.length() != 2) {
                        throw new RuntimeException("bad ISO-639-1 code: " + code_1);
                    }
                    String name = toks[6].trim();
                    Language l = new Language(code_3, code_1, name);
                    codeToLang.put(code_3, l);
                    if (code_1 != null) {
                        codeToLang.put(code_1, l);
                    }
                    nameToLang.put(name, l);
                }
            }
        } catch (IOException ex) {
            log.error("Initialization of languages code to name mapping failed.", ex);
        }
    }

    public static LanguagesISO693 getInstance() {
        if (instance == null) {
            instance = new LanguagesISO693();
        }
        return instance;
    }

    public Map<String, Language> getCodeToLangMap() {
        return Collections.unmodifiableMap(codeToLang);
    }

    public boolean isCode(String code) {
        return codeToLang.containsKey(code);
    }

    public String code_3ForCode(String code639_1) {
        if (code639_1 == null) {
            return null;
        }
        Language l = codeToLang.get(code639_1);
        if (l == null) {
            log.error("Unknown ISO-639-1 code: {}", code639_1);
            return null;
        }
        return l.code_3;
    }

    public String code_1ForCode_3(String code639_3) {
        if (code639_3 == null) {
            return null;
        }
        Language l = codeToLang.get(code639_3);
        if (l == null) {
            log.error("Unknown ISO-639-3 code: {}", code639_3);
            return null;
        }
        return l.code_1;
    }

    public String code_3ForName(String name) {
        Language l = nameToLang.get(name);
        if (l == null) {
            log.error("Unknown language name: {}", name);
            return null;
        }
        return l.code_3;
    }

    public String nameForCode(String code) {
        Language l = codeToLang.get(code);
        if (l == null) {
            log.error("Unknown language code: {}", code);
            return null;
        }
        return l.name;
    }

    public Map<String, String> getLanguageMap(Collection<String> codes) {
        Map<String, String> languages = new HashMap<String, String>();
        for (final String code : codes) {
            final String name = LanguagesISO693.getInstance().nameForCode(code);
            languages.put(code, name);
        }
        return languages;
    }
}
