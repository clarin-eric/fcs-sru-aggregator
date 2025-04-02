package eu.clarin.sru.fcs.aggregator.scan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import eu.clarin.sru.client.fcs.ClarinFCSEndpointDescription.DataView;
import eu.clarin.sru.client.fcs.ClarinFCSEndpointDescription.Layer;
import eu.clarin.sru.client.fcs.ClarinFCSEndpointDescription.LexField;
import eu.clarin.sru.client.fcs.ClarinFCSEndpointDescription.ResourceInfo.AvailabilityRestriction;
import eu.clarin.sru.client.fcs.DataViewAdvanced;
import eu.clarin.sru.client.fcs.DataViewLex;
import eu.clarin.sru.fcs.aggregator.util.LanguagesISO693;

/**
 * Represents information about resource, such as resource handle (id),
 * institution, title, description, language(s), etc. Also store the
 * information about resource sub-resources.
 *
 * @author Yana Panchenko
 */
public class Resource {
    public static final String ROOT_HANDLE = "root";
    public static final Pattern HANDLE_WITH_SPECIAL_CHARS = Pattern.compile(".*[<>=/()\\s].*");

    private Institution endpointInstitution;
    private Endpoint endpoint;

    private String handle;
    private Integer numberOfRecords;

    private Set<String> languages = new HashSet<>();
    private String landingPage;
    private String title;
    private String description;
    private String institution;

    private EnumSet<FCSSearchCapabilities> searchCapabilities = EnumSet.of(FCSSearchCapabilities.BASIC_SEARCH);
    private AvailabilityRestriction availabilityRestriction = AvailabilityRestriction.NONE;
    private List<DataView> availableDataViews;
    private List<Layer> availableLayers;
    private List<LexField> availableLexFields;

    public List<Resource> subResources = Collections.synchronizedList(new ArrayList<>());

    public Resource() {
    }

    public Resource(Institution institution, Endpoint endpoint) {
        this.endpointInstitution = institution;
        this.endpoint = endpoint;
    }

    public String getId() {
        return endpoint.getUrl() + "#" + handle;
    }

    public void addResource(Resource resource) {
        subResources.add(resource);
    }

    public List<Resource> getSubResources() {
        return Collections.unmodifiableList(subResources);
    }

    public void setSubResources(List<Resource> subResources) {
        this.subResources = subResources;
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

    public Institution getEndpointInstitution() {
        return endpointInstitution;
    }

    public void setEndpointInstitution(Institution institution) {
        this.endpointInstitution = institution;
    }

    public List<DataView> getAvailableDataViews() {
        return availableDataViews;
    }

    public void setAvailableDataViews(List<DataView> availableDataViews) {
        if (availableDataViews != null && !availableDataViews.isEmpty()) {
            this.availableDataViews = availableDataViews;
        } else {
            this.availableDataViews = null;
        }
    }

    public List<Layer> getAvailableLayers() {
        return availableLayers;
    }

    public void setAvailableLayers(List<Layer> availableLayers) {
        if (availableLayers != null && !availableLayers.isEmpty()) {
            this.availableLayers = availableLayers;
        } else {
            this.availableLayers = null;
        }
    }

    public List<LexField> getAvailableLexFields() {
        return availableLexFields;
    }

    public void setAvailableLexFields(List<LexField> availableLexFields) {
        if (availableLexFields != null && !availableLexFields.isEmpty()) {
            this.availableLexFields = availableLexFields;
        } else {
            this.availableLexFields = null;
        }
    }

    public void setAvailabilityRestriction(AvailabilityRestriction restriction) {
        this.availabilityRestriction = restriction;
    }

    public AvailabilityRestriction getAvailabilityRestriction() {
        return availabilityRestriction;
    }

    public boolean hasAvailabilityRestriction() {
        return !AvailabilityRestriction.NONE.equals(availabilityRestriction);
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
                // if (availableDataView.getMimeType().equals(DataViewHits.TYPE)) {
                // // NOTE: this dataview is required
                // resolvedSearchCapabilities.add(FCSSearchCapabilities.BASIC_SEARCH);
                // } else

                if (availableDataView.getMimeType().equals(DataViewAdvanced.TYPE)) {
                    resolvedSearchCapabilities.add(FCSSearchCapabilities.ADVANCED_SEARCH);
                } else if (availableDataView.getMimeType().equals(DataViewLex.TYPE)) {
                    resolvedSearchCapabilities.add(FCSSearchCapabilities.LEX_SEARCH);
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

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((endpoint == null) ? 0 : endpoint.hashCode());
        result = prime * result + ((handle == null) ? 0 : handle.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Resource other = (Resource) obj;
        if (endpoint == null) {
            if (other.endpoint != null)
                return false;
        } else if (!endpoint.equals(other.endpoint))
            return false;
        if (handle == null) {
            if (other.handle != null)
                return false;
        } else if (!handle.equals(other.handle))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Resource [endpoint=" + endpoint + ", handle=" + handle + "]";
    }
}
