package eu.clarin.sru.fcs.aggregator.sopt;

import java.util.HashSet;
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
    private String displayTerm;
    private Set<String> languages = new HashSet<String>();
    private String landingPage;
    private String title;
    private String description;
    boolean temp = false;

    public Corpus() {
        temp = true;
    }

    public Corpus(Institution institution, String endpointUrl) {
        this.institution = institution;
        this.endpointUrl = endpointUrl;
    }

    public boolean isTemporary() {
        return temp;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String value) {
        this.handle = value;
    }

    public void setNumberOfRecords(int numberOfRecords) {
        this.numberOfRecords = numberOfRecords;
    }

    public Integer getNumberOfRecords() {
        return numberOfRecords;
    }

    public String getDisplayName() {
        return displayTerm;
    }

    public void setDisplayName(String displayName) {
        this.displayTerm = displayName;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public Institution getInstitution() {
        return institution;
    }

    public Set<String> getLanguages() {
        return languages;
    }

    public void setLanguages(Set<String> languages) {
        this.languages = languages;
    }

    public void addLanguage(String language) {
        this.languages.add(language);
    }

    public String getLandingPage() {
        return landingPage;
    }

    public void setLandingPage(String landingPage) {
        this.landingPage = landingPage;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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
