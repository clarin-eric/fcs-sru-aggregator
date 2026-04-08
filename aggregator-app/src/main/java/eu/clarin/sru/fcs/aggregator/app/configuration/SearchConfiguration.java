package eu.clarin.sru.fcs.aggregator.app.configuration;

import org.hibernate.validator.constraints.Range;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SearchConfiguration {

    @Range
    @JsonProperty
    private int maxConcurrentRequests = 4;

    @Range
    @JsonProperty
    private int requestTimeoutMs = 30_000; // 30sec

    @Range
    @JsonProperty
    private int maxAmountSearchCache = 1_000;

    @Range
    @JsonProperty
    private long maxAgeSearchCacheMs = 3600_000; // 60min

    // ----------------------------------------------------------------------

    @JsonProperty("maxConcurrentRequests")
    public int getMaxConcurrentRequests() {
        return maxConcurrentRequests;
    }

    @JsonProperty("requestTimeoutMs")
    public int getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    @JsonProperty("maxAmountSearchCache")
    public int getMaxAmountSearchCache() {
        return maxAmountSearchCache;
    }

    @JsonProperty("maxAgeSearchCacheMs")
    public long getMaxAgeSearchCacheMs() {
        return maxAgeSearchCacheMs;
    }

}
