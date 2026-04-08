package eu.clarin.sru.fcs.aggregator.app.configuration;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Configuration extends io.dropwizard.core.Configuration {

    @Valid
    @JsonProperty
    private AggregatorConfiguration aggregator;

    @JsonProperty("aggregator")
    public synchronized AggregatorConfiguration getAggregatorConfiguration() {
        return aggregator;
    }

}
