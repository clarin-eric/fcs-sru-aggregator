package eu.clarin.sru.fcs.aggregator.lang;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.LoggerFactory;

/**
 * Represents collection of languages.
 *
 * @author Yana Panchenko
 */
public class LanguagesISO693_3 {

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(LanguagesISO693_3.class);
	public static final String LANGUAGES_FILE_PATH = "/lang/iso-639-3_20140320.tab";
	public static final String LANGUAGES_FILE_ENCODING = "UTF-8";

	private static LanguagesISO693_3 instance = null;

	public static class Language {

		// code is ISO-639-3 (3 letters) while code_2 is ISO-639-1 (2 letters)
		String code_3, code_1, name;

		public Language(String code_3, String code_1, String name) {
			this.code_3 = code_3;
			this.code_1 = code_1;
			this.name = name;
		}
	}

	private Map<String, Language> code_3ToLang = new HashMap<String, Language>();
	private Map<String, Language> nameToLang = new HashMap<String, Language>();
	private Map<String, Language> code_1ToLang = new HashMap<String, Language>();

	private LanguagesISO693_3() {
		InputStream is = LanguagesISO693_3.class.getResourceAsStream(LANGUAGES_FILE_PATH);
		try (BufferedReader br = new BufferedReader(new InputStreamReader(is, LANGUAGES_FILE_ENCODING))) {
			String line = br.readLine(); // ignore first line
			while ((line = br.readLine()) != null) {
				if (line.length() > 0) {
					String[] toks = line.split("\\t");
					if (toks.length != 7 && toks.length != 8) {
						log.error("Line error in language codes file: ", line);
						continue;
					}
					String code_3 = toks[0].trim();
					String code_1 = toks[3].trim().isEmpty() ? null : toks[3].trim();
					if (code_1 != null && code_1.length() != 2) {
						throw new RuntimeException("bad ISO-639-1 code: " + code_1);
					}
					String name = toks[6].trim();
					Language l = new Language(code_3, code_1, name);
					code_3ToLang.put(code_3, l);
					if (code_1 != null) {
						code_1ToLang.put(code_1, l);
					}
					nameToLang.put(name, l);
				}
			}
		} catch (IOException ex) {
			log.error("Initialization of languages code to name mapping failed.", ex);
		}

		ObjectWriter ow = new ObjectMapper().writerWithDefaultPrettyPrinter();
		try {
			System.out.println(ow.writeValueAsString(code_3ToLang));
		} catch (JsonProcessingException ex) {
		}
	}

	public static LanguagesISO693_3 getInstance() {
		if (instance == null) {
			instance = new LanguagesISO693_3();
		}
		return instance;
	}

	public Set<String> getCodes_3() {
		return code_3ToLang.keySet();
	}

	public String code_3ForCode_1(String code639_1) {
		if (code639_1 == null) {
			return null;
		}
		Language l = code_1ToLang.get(code639_1);
		if (l == null) {
			log.error("Unknown ISO-639-1 code: " + code639_1);
			return null;
		}
		return l.code_3;
	}

	public String code_1ForCode_3(String code639_3) {
		if (code639_3 == null) {
			return null;
		}
		Language l = code_3ToLang.get(code639_3);
		if (l == null) {
			log.error("Unknown ISO-639-3 code: " + code639_3);
			return null;
		}
		return l.code_1;
	}

	public String code_3ForName(String name) {
		Language l = nameToLang.get(name);
		if (l == null) {
			log.error("Unknown language name: " + name);
			return null;
		}
		return l.code_3;
	}

	public String nameForCode_3(String code) {
		Language l = code_3ToLang.get(code);
		if (l == null) {
			log.error("Unknown language code: " + code);
			return null;
		}
		return l.name;
	}

}
