package eu.clarin.sru.fcs.aggregator.app.rest;

import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import eu.clarin.sru.fcs.aggregator.scan.Statistics.EndpointStats;

public class EndpointStatistics {
    @JsonProperty(required = true)
    double timeout;

    @JsonProperty(required = true)
    Boolean isScan;

    @JsonProperty(required = true)
    Map<String, Map<String, EndpointStats>> institutions;

    @JsonProperty(required = true)
    Date date;

    public EndpointStatistics() {
    }

    public EndpointStatistics(Boolean isScan, Map<String, Map<String, EndpointStats>> institutions, Date date,
            double timeout) {
        this.isScan = isScan;
        this.institutions = institutions;
        this.date = date;
        this.timeout = timeout;
    }

} // class EndpointStatistics
