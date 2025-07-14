package eu.clarin.sru.fcs.aggregator.app.serialization;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class ResourceMixin {
    @JsonProperty
    private Map<String, String> title;
    @JsonProperty
    private Map<String, String> description;
    @JsonProperty
    private Map<String, String> institution;

    // disable string setter from deserialization
    @JsonIgnore
    public abstract void setTitle(String title);
    @JsonIgnore
    public abstract void setDescription(String description);
    @JsonIgnore
    public abstract void setInstitution(String institution);
}
