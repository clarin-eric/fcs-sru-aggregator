package eu.clarin.sru.fcs.aggregator.app.configuration;

import java.net.URL;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Range;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import eu.clarin.sru.fcs.aggregator.scan.EndpointOverrideConfig;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonPropertyOrder({ "url", "name", "website", "enabled", "overrideOnly", "isCQL", "maxConcurrentSearchRequests" })
public class EndpointOverrideConfiguration implements EndpointOverrideConfig {

    @NotNull
    @JsonProperty
    private URL url;

    @JsonProperty
    private Map<String, String> name;

    @JsonProperty
    private String website;

    @JsonProperty
    private boolean enabled = true;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty
    private boolean overrideOnly = true;

    @JsonProperty
    private boolean isCQL = false;

    @Range(min = -1)
    @JsonProperty
    private int maxConcurrentSearchRequests = -1;

    // ----------------------------------------------------------------------

    @JsonProperty("url")
    @Override
    public URL getUrl() {
        return url;
    }

    @JsonProperty("name")
    @Override
    public Map<String, String> getName() {
        return name;
    }

    @JsonProperty("website")
    @Override
    public String getWebsite() {
        return website;
    }

    @JsonProperty("enabled")
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @JsonProperty("overrideOnly")
    @Override
    public boolean isOverrideOnly() {
        return overrideOnly;
    }

    @JsonProperty("isCQL")
    @Override
    public boolean isCQL() {
        return isCQL;
    }

    @JsonProperty("maxConcurrentSearchRequests")
    @Override
    public int getMaxConcurrentSearchRequests() {
        return maxConcurrentSearchRequests;
    }

}
