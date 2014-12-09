package eu.clarin.sru.fcs.aggregator.scan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the cached scan data (endpoints descriptions) that stores
 * the cache in memory in maps.
 *
 * @author yanapanchenko
 * @author edima
 */
public class Corpora {

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(Corpora.class);

	private Map<String, Set<Corpus>> langToRootCorpora = new HashMap<String, Set<Corpus>>();
	private Map<String, Set<Corpus>> langToTopUniqueCorpora = new HashMap<String, Set<Corpus>>();
	private Map<String, Diagnostic> endpointDiagnostics = Collections.synchronizedMap(new HashMap<String, Diagnostic>());
	private List<Institution> institutions = Collections.synchronizedList(new ArrayList<Institution>());
	private List<Corpus> corpora = new ArrayList<Corpus>();

	public List<Institution> getInstitutions() {
		return Collections.unmodifiableList(institutions);
	}

	public List<Corpus> getCorpora() {
		return Collections.unmodifiableList(corpora);
	}

	public Map<String, Diagnostic> getEndpointDiagnostics() {
		return Collections.unmodifiableMap(endpointDiagnostics);
	}

	public void addInstitution(Institution institution) {
		institutions.add(institution);
	}

	public synchronized boolean addCorpus(Corpus c, Corpus parentCorpus) {
		if (findByHandle(c.getHandle()) != null) {
			return false;
		}
		if (parentCorpus == null) { //i.e it's a root corpus
			corpora.add(c);
			for (String lang : c.getLanguages()) {
				if (!langToRootCorpora.containsKey(lang)) {
					langToRootCorpora.put(lang, new HashSet<Corpus>());
				}
				langToRootCorpora.get(lang).add(c);
			}
		} else {
			parentCorpus.addCorpus(c);
		}

		// index top corpora with unique language as for their languages
		if (c.getLanguages().size() == 1
				&& (parentCorpus == null || parentCorpus.getLanguages().size() > 0)) {
			String lang = c.getLanguages().iterator().next();
			if (!langToTopUniqueCorpora.containsKey(lang)) {
				langToTopUniqueCorpora.put(lang, new LinkedHashSet<Corpus>());
			}
			langToTopUniqueCorpora.get(lang).add(c);
		}
		return true;
	}

	public void addEndpointDiagnostic(String endpoint, Diagnostic diagnostic) {
		endpointDiagnostics.put(endpoint, diagnostic);
	}

	public Set<String> getLanguages() {
		Set<String> languages = new HashSet<String>(this.langToRootCorpora.size());
		languages.addAll(this.langToRootCorpora.keySet());
		return languages;
	}

	public List<Corpus> getRootCorporaForLang(String lang) {
		List<Corpus> ret = new ArrayList<Corpus>();
		for (Corpus c : corpora) {
			if (c.getLanguages().contains(lang)) {
				ret.add(c);
			}
		}
		return ret;
	}

	public List<Corpus> getTopUniqueLanguageCorpora(String lang) {
		ArrayList<Corpus> corpora = new ArrayList<Corpus>();
		corpora.addAll(langToTopUniqueCorpora.get(lang));
		return corpora;
	}

	@Override
	public String toString() {
		return "corpora{\n" + "institutions=" + institutions + "\n"
				+ "\n corpora=" + corpora + "\n}";
	}

	public List<Corpus> findByEndpoint(final String endpointUrl) {
		final List<Corpus> found = new ArrayList<Corpus>();
		visit(corpora, new CallCorpus() {
			@Override
			public void call(Corpus c) {
				if (c.getEndpointUrl().equals(endpointUrl)) {
					found.add(c);
				}
			}
		});
		return found;
	}

	public Corpus findByHandle(final String handle) {
		final List<Corpus> found = new ArrayList<Corpus>();
		visit(corpora, new CallCorpus() {
			@Override
			public void call(Corpus c) {
				if (c.getHandle() != null && c.getHandle().equals(handle)) {
					found.add(c);
				}
			}
		});
		return found.isEmpty() ? null : found.get(0);
	}

	public List<Corpus> getCorporaByIds(final Set<String> corporaIds) {
		final List<Corpus> found = new ArrayList<Corpus>();
		visit(corpora, new CallCorpus() {
			@Override
			public void call(Corpus c) {
				if (corporaIds.contains(c.getId())) {
					found.add(c);
				}
			}
		});
		return found;
	}

	public static interface CallCorpus {
		void call(Corpus c);
	}

	private static void visit(List<Corpus> corpora, CallCorpus clb) {
		for (Corpus c : corpora) {
			clb.call(c);
			visit(c.getSubCorpora(), clb);
		}
	}
}
