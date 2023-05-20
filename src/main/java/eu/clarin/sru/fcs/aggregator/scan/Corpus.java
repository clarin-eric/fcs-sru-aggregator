package eu.clarin.sru.fcs.aggregator.scan;

import eu.clarin.sru.client.fcs.ClarinFCSEndpointDescription.DataView;
import eu.clarin.sru.client.fcs.ClarinFCSEndpointDescription.Layer;
import eu.clarin.sru.fcs.aggregator.util.LanguagesISO693;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.LoggerFactory;

/**
 * Represents information about corpus resource, such as corpus handle (id),
 * institution, title, description, language(s), etc. Also store the
 * information about corpus sub-corpora.
 *
 * @author Yana Panchenko
 */
public class Corpus {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Corpus.class);

    public static final String ROOT_HANDLE = "root";
    public static final Pattern HANDLE_WITH_SPECIAL_CHARS = Pattern.compile(".*[<>=/()\\s].*");

    public static final String MIMETYPE_DATAVIEW_HITS = "application/x-clarin-fcs-hits+xml";
    public static final String MIMETYPE_DATAVIEW_ADVANCED = "application/x-clarin-fcs-adv+xml";

    private Institution institution;
    private Endpoint endpoint;
    private String handle;
    private Integer numberOfRecords;
    private Set<String> languages = new HashSet<String>();
    private String landingPage;
    private String title;
    private String description;
    private EnumSet<FCSSearchCapabilities> searchCapabilities = EnumSet.of(FCSSearchCapabilities.BASIC_SEARCH);
    private List<DataView> availableDataViews;
    private List<Layer> availableLayers;
    public List<Corpus> subCorpora = Collections.synchronizedList(new ArrayList<Corpus>());

    public Corpus() {
    }

    public Corpus(Institution institution, Endpoint endpoint) {
        this.institution = institution;
        this.endpoint = endpoint;
    }

    public String getId() {
        return endpoint.getUrl() + "#" + handle;
    }

    public void setId(String id) { // dumb setter for JsonDeserialization
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

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public List<DataView> getAvailableDataViews() {
        return availableDataViews;
    }

    public void setAvailableDataViews(List<DataView> availableDataViews) {
        this.availableDataViews = availableDataViews;
    }

    public List<Layer> getAvailableLayers() {
        return availableLayers;
    }

    public void setAvailableLayers(List<Layer> availableLayers) {
        this.availableLayers = availableLayers;
    }

    public void setSearchCapabilities(EnumSet<FCSSearchCapabilities> searchCapabilities) {
        this.searchCapabilities = searchCapabilities;
    }

    public EnumSet<FCSSearchCapabilities> getSearchCapabilities() {
        return searchCapabilities;
    }

    public EnumSet<FCSSearchCapabilities> getSearchCapabilitiesResolved() {
        EnumSet<FCSSearchCapabilities> resolvedSearchCapabilities = EnumSet.of(FCSSearchCapabilities.BASIC_SEARCH);

        if (availableDataViews != null && !availableDataViews.isEmpty()) {
            for (DataView availableDataView : availableDataViews) {
                // if (availableDataView.getMimeType().equals(MIMETYPE_DATAVIEW_HITS)) {
                // // NOTE: this dataview is required
                // resolvedSearchCapabilities.add(FCSSearchCapabilities.BASIC_SEARCH);
                // } else
                if (availableDataView.getMimeType().equals(MIMETYPE_DATAVIEW_ADVANCED)) {
                    resolvedSearchCapabilities.add(FCSSearchCapabilities.ADVANCED_SEARCH);
                }
            }
        }

        // build intersection
        resolvedSearchCapabilities.retainAll(this.searchCapabilities);

        return resolvedSearchCapabilities;
    }

    public Set<String> getLanguages() {
        return languages;
    }

    public void setLanguages(Set<String> languages) {
        this.languages = languages;
    }

    public void addLanguage(String language) {
        if (LanguagesISO693.getInstance().isCode(language)) {
            this.languages.add(language);
        } else {
            String code = LanguagesISO693.getInstance().code_3ForName(language);
            this.languages.add(code == null ? language : code);
        }
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

    void setTitle(String title) {
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
        hash = 29 * hash + (this.endpoint != null ? this.endpoint.hashCode() : 0);
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
        if ((this.endpoint == null) ? (other.endpoint != null) : !this.endpoint.equals(other.endpoint)) {
            return false;
        }
        if ((this.handle == null) ? (other.handle != null) : !this.handle.equals(other.handle)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Corpus{" + "endpoint=" + endpoint + ", handle=" + handle + '}';
    }
}
