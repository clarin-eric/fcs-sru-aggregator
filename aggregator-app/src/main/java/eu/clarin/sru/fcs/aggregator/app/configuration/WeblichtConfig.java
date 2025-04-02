package eu.clarin.sru.fcs.aggregator.app.configuration;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class WeblichtConfig {
    @JsonProperty
    String url;

    @JsonProperty
    String exportServerUrl;

    @JsonProperty
    List<String> acceptedTcfLanguages;

    @JsonIgnore
    public String getUrl() {
        return url;
    }

    @JsonIgnore
    public String getExportServerUrl() {
        return exportServerUrl;
    }

    @JsonIgnore
    public List<String> getAcceptedTcfLanguages() {
        return acceptedTcfLanguages;
    }

    public void setAcceptedTcfLanguages(List<String> acceptedTcfLanguages) {
        this.acceptedTcfLanguages = acceptedTcfLanguages;
    }
}
