package eu.clarin.sru.fcs.aggregator.lang;

import eu.clarin.sru.fcs.aggregator.lang.LanguageISO693_2;
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
public class LanguagesISO693_2 {

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(LanguagesISO693_2.class);

	private static LanguagesISO693_2 instance = null;

	private Map<String, LanguageISO693_2> code2Lang = new HashMap<String, LanguageISO693_2>();
	private Map<String, LanguageISO693_2> name2Lang = new HashMap<String, LanguageISO693_2>();

//	public static final String LANGUAGES_FILE_PATH = "/lang/ISO-639-2_utf-8.txt";
	public static final String LANGUAGES_FILE_PATH = "/lang/ISO-639-3_20140320.tab";
	public static final String LANGUAGES_FILE_ENCODING = "UTF-8";

	private LanguagesISO693_2() {
		loadMapping();
	}

	public static LanguagesISO693_2 getInstance() {
		if (instance == null) {
			instance = new LanguagesISO693_2();
		}
		return instance;
	}

	/**
	 * Gets language by its ISO 639 language code.
	 *
	 * @param code ISO 639/1, 639/2T or 639/2B language code
	 * @return language
	 */
	public LanguageISO693_2 langForCode(String code) {
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
	 * Gets language by its name
	 *
	 * @param code ISO 639/1, 639/2T or 639/2B language code
	 * @return language
	 */
	public LanguageISO693_2 langForName(String name) {
		return this.name2Lang.get(name);
	}

	/**
	 * Gets language code by its english name
	 */
	public String codeForName(String name) {
		if (this.name2Lang.containsKey(name)) {
			return this.name2Lang.get(name).getCode();
		}
		return name;
	}

	/**
	 * Gets all known to it ISO 639/1, 639/2T and 639/2B language codes.
	 *
	 *
	 * @return language codes
	 */
	public Set<String> getCodes() {
		return this.code2Lang.keySet();
	}

	private void loadMapping() {
		InputStream is = LanguagesISO693_2.class.getResourceAsStream(LANGUAGES_FILE_PATH);
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

//					String[] splitted = line.split("\\|");
//					String alpha3b = splitted[0];
//					String alpha3t = splitted[1];
//					String alpha2 = splitted[2];
//					String enName = splitted[3];
//					Language lang = new Language(alpha2, alpha3t, alpha3b, enName);
//					this.name2Lang.put(enName, lang);
//					if (!alpha3b.isEmpty()) {
//						this.code2Lang.put(alpha3b, lang);
//					}
//					if (!alpha3t.isEmpty()) {
//						this.code2Lang.put(alpha3t, lang);
//					}
//					if (!alpha2.isEmpty()) {
//						this.code2Lang.put(alpha2, lang);
//					}
				}
			}
		} catch (IOException ex) {
			log.error("Initialization of languages code to name mapping failed.", ex);
		}
		//		ObjectWriter ow = new ObjectMapper().writerWithDefaultPrettyPrinter();
		//		try {
		//			System.out.println(ow.writeValueAsString(code2Lang));
		//		} catch (JsonProcessingException ex) {
	}

	private void loadMappingISO693_2() {
		InputStream is = LanguagesISO693_2.class.getResourceAsStream(LANGUAGES_FILE_PATH);
		try (BufferedReader br = new BufferedReader(new InputStreamReader(is, LANGUAGES_FILE_ENCODING))) {
			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.length() > 0) {
					String[] splitted = line.split("\\|");
					String alpha3b = splitted[0];
					String alpha3t = splitted[1];
					String alpha2 = splitted[2];
					String enName = splitted[3];
					LanguageISO693_2 lang = new LanguageISO693_2(alpha2, alpha3t, alpha3b, enName);
					this.name2Lang.put(enName, lang);
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
			log.error("Initialization of languages code to name mapping failed.", ex);
		}
	}
}
