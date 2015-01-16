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

		// code is ISO-639-3 (3 letters) while code_2 is ISO-639-2 (2 letters)
		String code, code_2, name;

		public Language(String code, String code_2, String name) {
			this.code = code;
			this.code_2 = code_2;
			this.name = name;
		}
	}

	private Map<String, Language> codeToLang = new HashMap<String, Language>();
	private Map<String, Language> nameToLang = new HashMap<String, Language>();
	private Map<String, Language> code_2ToLang = new HashMap<String, Language>();

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
					String code = toks[0].trim();
					String code_2 = toks[3].trim().isEmpty() ? null : toks[3].trim();
					if (code_2 != null && code_2.length() != 2) {
						throw new RuntimeException("bad code_2 code: " + code_2);
					}
					String name = toks[6].trim();
					Language l = new Language(code, code_2, name);
					codeToLang.put(code, l);
					if (code_2 != null) {
						code_2ToLang.put(code_2, l);
					}
					nameToLang.put(name, l);
				}
			}
		} catch (IOException ex) {
			log.error("Initialization of languages code to name mapping failed.", ex);
		}

		ObjectWriter ow = new ObjectMapper().writerWithDefaultPrettyPrinter();
		try {
			System.out.println(ow.writeValueAsString(codeToLang));
		} catch (JsonProcessingException ex) {
		}
	}

	public static LanguagesISO693_3 getInstance() {
		if (instance == null) {
			instance = new LanguagesISO693_3();
		}
		return instance;
	}

	public Set<String> getCodes() {
		return codeToLang.keySet();
	}

	public String codeForCode639_2(String code639_2) {
		if (code639_2 == null) {
			return null;
		}
		Language l = code_2ToLang.get(code639_2);
		if (l == null) {
			log.error("Unknown 639-2 code: " + code639_2);
			return null;
		}
		return l.code;
	}

	public String codeForName(String name) {
		Language l = nameToLang.get(name);
		if (l == null) {
			log.error("Unknown language name: " + name);
			return null;
		}
		return l.code;
	}

	public String nameForCode(String code) {
		Language l = codeToLang.get(code);
		if (l == null) {
			log.error("Unknown language code: " + code);
			return null;
		}
		return l.name;
	}

}
