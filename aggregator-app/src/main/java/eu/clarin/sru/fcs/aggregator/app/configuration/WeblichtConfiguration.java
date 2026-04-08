package eu.clarin.sru.fcs.aggregator.app.configuration;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class WeblichtConfiguration {

    @NotNull
    @JsonProperty
    private String url = "https://weblicht.sfs.uni-tuebingen.de/weblicht/?input=";

    @NotNull
    @JsonProperty
    private String exportServerUrl;

    @NotNull
    @JsonProperty
    private List<String> acceptedTcfLanguages = List.of("en", "de", "nl", "fr", "it", "es", "pl");

    // ----------------------------------------------------------------------

    @JsonProperty("url")
    public String getUrl() {
        return url;
    }

    @JsonProperty("exportServerUrl")
    public String getExportServerUrl() {
        return exportServerUrl;
    }

    @JsonProperty("acceptedTcfLanguages")
    public List<String> getAcceptedTcfLanguages() {
        return acceptedTcfLanguages;
    }

    @JsonProperty("acceptedTcfLanguages")
    public void setAcceptedTcfLanguages(List<String> acceptedTcfLanguages) {
        this.acceptedTcfLanguages = acceptedTcfLanguages;
    }

}
