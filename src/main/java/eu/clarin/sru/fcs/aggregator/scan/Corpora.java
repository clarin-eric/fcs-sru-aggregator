package eu.clarin.sru.fcs.aggregator.scan;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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

	@JsonProperty
	private List<Institution> institutions = Collections.synchronizedList(new ArrayList<Institution>());
	@JsonProperty
	private List<Corpus> corpora = new ArrayList<Corpus>();

	public List<Institution> getInstitutions() {
		return Collections.unmodifiableList(institutions);
	}

	public List<Corpus> getCorpora() {
		return Collections.unmodifiableList(corpora);
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
		} else {
			parentCorpus.addCorpus(c);
		}
		return true;
	}

	public Set<String> getLanguages() {
		final Set<String> languages = new HashSet<String>();
		visit(corpora, new CallCorpus() {
			@Override
			public void call(Corpus c) {
				languages.addAll(c.getLanguages());
			}
		});
		return languages;
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

	public static interface CallCorpus {

		void call(Corpus c);
	}

	private static void visit(List<Corpus> corpora, CallCorpus clb) {
		for (Corpus c : corpora) {
			clb.call(c);
			visit(c.getSubCorpora(), clb);
		}
	}

	@Override
	public String toString() {
		return "corpora{\n" + "institutions=" + institutions + "\n"
				+ "\n corpora=" + corpora + "\n}";
	}

}
