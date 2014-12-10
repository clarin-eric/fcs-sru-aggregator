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

		String code, name;

		public Language(String code, String name) {
			this.code = code;
			this.name = name;
		}
	}

	private Map<String, Language> code2Lang = new HashMap<String, Language>();
	private Map<String, Language> name2Lang = new HashMap<String, Language>();

	private LanguagesISO693_3() {
		InputStream is = LanguagesISO693_3.class.getResourceAsStream(LANGUAGES_FILE_PATH);
		try (BufferedReader br = new BufferedReader(new InputStreamReader(is, LANGUAGES_FILE_ENCODING))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.length() > 0) {
					String[] toks = line.split("\\t");
					if (toks.length != 7 && toks.length != 8) {
						log.error("Line error in language codes file: ", line);
						continue;
					}
					String code = toks[0];
					String name = toks[6];
					Language l = new Language(code, name);
					code2Lang.put(code, l);
					name2Lang.put(name, l);
				}
			}
		} catch (IOException ex) {
			log.error("Initialization of languages code to name mapping failed.", ex);
		}

		ObjectWriter ow = new ObjectMapper().writerWithDefaultPrettyPrinter();
		try {
			System.out.println(ow.writeValueAsString(code2Lang));
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
		return code2Lang.keySet();
	}

	public String codeForName(String name) {
		Language l = name2Lang.get(name);
		if (l == null) {
			log.error("Unknown language name: " + name);
			return null;
		}
		return l.code;
	}

	public String nameForCode(String code) {
		Language l = code2Lang.get(code);
		if (l == null) {
			log.error("Unknown language code: " + code);
			return null;
		}
		return l.name;
	}

}
