package eu.clarin.sru.fcs.aggregator.app.configuration;

import java.util.Collections;
import java.util.List;

import org.hibernate.validator.constraints.Range;

import com.fasterxml.jackson.annotation.JsonProperty;

import eu.clarin.sru.fcs.aggregator.app.util.IgnoreNotNullIfNotEnabled;

@IgnoreNotNullIfNotEnabled
public final class MatomoConfiguration {

    @JsonProperty
    private boolean enabled = false;

    @IgnoreNotNullIfNotEnabled.IgnorableNotNull
    @JsonProperty
    private String url;

    @Range(min = -1)
    @JsonProperty
    private int siteID = -1;

    @JsonProperty
    private List<String> setDomains;

    // ----------------------------------------------------------------------

    @JsonProperty("enabled")
    public boolean isEnabled() {
        return enabled;
    }

    @JsonProperty("url")
    public String getUrl() {
        return url;
    }

    @JsonProperty("siteID")
    public int getSiteID() {
        return siteID;
    }

    @JsonProperty("setDomains")
    public List<String> getSetDomains() {
        if (setDomains == null) {
            return Collections.emptyList();
        }
        return setDomains;
    }

}
