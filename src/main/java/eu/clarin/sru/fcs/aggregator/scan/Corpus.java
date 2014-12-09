package eu.clarin.sru.fcs.aggregator.scan;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eu.clarin.sru.fcs.aggregator.lang.LanguagesISO693_3;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Represents information about corpus resource, such as corpus handle (id),
 * institution, title, description, language(s), etc. Does not store the
 * information about corpus sub-corpora.
 *
 * @author Yana Panchenko
 */
public class Corpus {

	public static final String ROOT_HANDLE = "root";
	public static final Pattern HANDLE_WITH_SPECIAL_CHARS = Pattern.compile(".*[<>=/()\\s].*");

	private Institution institution;
	private String endpointUrl;
	private String handle;
	private Integer numberOfRecords;
	private String displayName;
	private Set<String> languages = new HashSet<String>();
	private String landingPage;
	private String title;
	private String description;
	public List<Corpus> subCorpora = Collections.synchronizedList(new ArrayList<Corpus>());

	public Corpus() {
	}

	public Corpus(Institution institution, String endpointUrl) {
		this.institution = institution;
		this.endpointUrl = endpointUrl;
	}

	@JsonIgnore
	public String getId() {
		return endpointUrl + "#" + handle;
	}

	public void addCorpus(Corpus c) {
		subCorpora.add(c);
	}

	public List<Corpus> getSubCorpora() {
		return Collections.unmodifiableList(subCorpora);
	}

	public void setSubCorpora(List<Corpus> subCorpora) {
		this.subCorpora = subCorpora;
	}


	public String getHandle() {
		return handle;
	}

	public void setHandle(String value) {
		this.handle = value;
	}

	public Integer getNumberOfRecords() {
		return numberOfRecords;
	}

	public void setNumberOfRecords(Integer numberOfRecords) {
		this.numberOfRecords = numberOfRecords;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getEndpointUrl() {
		return endpointUrl;
	}

	public void setEndpointUrl(String endpointUrl) {
		this.endpointUrl = endpointUrl;
	}

	public Institution getInstitution() {
		return institution;
	}

	public void setInstitution(Institution institution) {
		this.institution = institution;
	}

	public Set<String> getLanguages() {
		return languages;
	}

	public void setLanguages(Set<String> languages) {
		this.languages = languages;
	}

	public void addLanguage(String language) {
		if (LanguagesISO693_3.getInstance().getCodes().contains(language)) {
			this.languages.add(language);
		} else {
			String code = LanguagesISO693_3.getInstance().codeForName(language);
			if (code != null) {
				this.languages.add(code);
			} else {
				this.languages.add(language);
			}
		}
	}

	public String getLandingPage() {
		return landingPage;
	}

	public void setLandingPage(String landingPage) {
		this.landingPage = landingPage;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 29 * hash + (this.endpointUrl != null ? this.endpointUrl.hashCode() : 0);
		hash = 29 * hash + (this.handle != null ? this.handle.hashCode() : 0);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Corpus other = (Corpus) obj;
		if ((this.endpointUrl == null) ? (other.endpointUrl != null) : !this.endpointUrl.equals(other.endpointUrl)) {
			return false;
		}
		if ((this.handle == null) ? (other.handle != null) : !this.handle.equals(other.handle)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "Corpus{" + "endpointUrl=" + endpointUrl + ", handle=" + handle + '}';
	}

}
