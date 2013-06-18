package eu.clarin.sru.fcs.aggregator.sopt;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 *
 * @author Yana Panchenko
 */
public class Corpus {

    public static final String ROOT_HANDLE = "root";
    public static final Pattern HANDLE_WITH_SPECIAL_CHARS = Pattern.compile(".*[<>=/()\\s].*");
    private InstitutionI institution;
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

    public Corpus(InstitutionI institution, String endpointUrl) {
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

    public InstitutionI getInstitution() {
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

}
