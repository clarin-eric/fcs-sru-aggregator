package eu.clarin.sru.fcs.aggregator.app.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScanSearchStatistics {

    @JsonProperty(value = "Last Scan", required = true)
    EndpointStatistics scans;

    @JsonProperty(value = "Recent Searches", required = true)
    EndpointStatistics searches;

    public ScanSearchStatistics() {
    }

    public ScanSearchStatistics(EndpointStatistics scans, EndpointStatistics searches) {
        this.scans = scans;
        this.searches = searches;
    }

} // class ScanSearchStatistics
