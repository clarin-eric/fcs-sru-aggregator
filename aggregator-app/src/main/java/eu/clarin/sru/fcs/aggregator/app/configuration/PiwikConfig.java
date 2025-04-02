package eu.clarin.sru.fcs.aggregator.app.configuration;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class PiwikConfig {
    @JsonProperty
    boolean enabled;

    @JsonProperty
    String url;

    @JsonProperty
    int siteID;

    @JsonProperty
    List<String> setDomains;

    @JsonIgnore
    public boolean isEnabled() {
        return enabled;
    }

    @JsonIgnore
    public String getUrl() {
        return url;
    }

    @JsonIgnore
    public int getSiteID() {
        return siteID;
    }

    @JsonIgnore
    public List<String> getSetDomains() {
        return setDomains;
    }
}
