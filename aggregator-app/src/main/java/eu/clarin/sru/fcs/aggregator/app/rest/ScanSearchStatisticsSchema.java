package eu.clarin.sru.fcs.aggregator.app.rest;

import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import eu.clarin.sru.fcs.aggregator.scan.Statistics.EndpointStats;

public class ScanSearchStatisticsSchema {
    public static class StatisticsSchema {
        @JsonProperty(required = true)
        int timeout;

        @JsonProperty(required = true)
        Boolean isScan;

        @JsonProperty(required = true)
        Map<String, Map<String, EndpointStats>> institutions;

        @JsonProperty(required = true)
        Date date;
    }

    @JsonProperty(value = "Last Scan", required = true)
    StatisticsSchema scans;

    @JsonProperty(value = "Recent Searches", required = true)
    StatisticsSchema searches;
}
