package eu.clarin.sru.fcs.aggregator.app.serialization;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class ResourceMixin {
    @JsonProperty
    Map<String, String> title;
    @JsonProperty
    Map<String, String> description;
    @JsonProperty
    Map<String, String> institution;

    // disable string setter from deserialization
    @JsonIgnore
    abstract void setTitle(String title);
    @JsonIgnore
    abstract void setDescription(String description);
    @JsonIgnore
    abstract void setInstitution(String institution);
}
