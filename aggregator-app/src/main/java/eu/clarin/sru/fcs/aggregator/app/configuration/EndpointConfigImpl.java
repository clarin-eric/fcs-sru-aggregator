package eu.clarin.sru.fcs.aggregator.app.configuration;

import java.net.MalformedURLException;
import java.net.URL;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import eu.clarin.sru.fcs.aggregator.scan.EndpointConfig;

public final class EndpointConfigImpl implements EndpointConfig {
    @NotNull
    @JsonProperty
    private URL url;

    @JsonProperty
    private String name;

    @JsonProperty
    private String website;

    public EndpointConfigImpl(String url) throws MalformedURLException {
        this.url = new URL(url);
    }

    public EndpointConfigImpl() {
    }

    @JsonIgnore
    @Override
    public URL getUrl() {
        return url;
    }

    @JsonIgnore
    @Override
    public String getName() {
        return name;
    }

    @JsonIgnore
    @Override
    public String getWebsite() {
        return website;
    }
}
