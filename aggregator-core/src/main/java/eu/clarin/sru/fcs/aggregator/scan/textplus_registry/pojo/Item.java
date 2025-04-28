package eu.clarin.sru.fcs.aggregator.scan.textplus_registry.pojo;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Item<T> {
    String entityId;
    String uniqueId;

    String creation;
    String definition_name;
    boolean current_version;
    int definition_version;

    boolean readOnly;
    boolean published;
    boolean draft;
    boolean valid;
    boolean template;
    boolean deleted;
    boolean composite;
    boolean imported;

    T properties;

    // @JsonProperty(value = "_presentation", required = false)
    // Object presentation;

    @JsonProperty(value = "_links", required = false)
    Map<String, Object> links;

    public String getEntityId() {
        return entityId;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public String getCreation() {
        return creation;
    }

    public String getDefinition_name() {
        return definition_name;
    }

    public boolean isCurrent_version() {
        return current_version;
    }

    public int getDefinition_version() {
        return definition_version;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public boolean isPublished() {
        return published;
    }

    public boolean isDraft() {
        return draft;
    }

    public boolean isValid() {
        return valid;
    }

    public boolean isTemplate() {
        return template;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public boolean isComposite() {
        return composite;
    }

    public boolean isImported() {
        return imported;
    }

    public T getProperties() {
        return properties;
    }

    public Map<String, Object> getLinks() {
        return links;
    }

}
