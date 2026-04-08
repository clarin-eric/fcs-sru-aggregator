package eu.clarin.sru.fcs.aggregator.app.configuration;

import java.util.concurrent.TimeUnit;

import org.hibernate.validator.constraints.Range;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScanConfiguration {

    @JsonProperty
    private String centreRegistryUrl = null;

    @Range
    @JsonProperty
    private int maxScanDepth = 3;
    // recommended 3

    @Range
    @JsonProperty
    private long scanTaskInitialDelay = 0;

    @Range
    @JsonProperty
    private long scanTaskInterval = 12;

    @JsonProperty
    private TimeUnit scanTaskIntervalTimeUnit = TimeUnit.HOURS;

    @Range
    @JsonProperty
    private int maxConcurrentRequests = 4;
    // @depth2: 1=361s; 2=225s; 4=207s

    @Range
    @JsonProperty
    private int requestTimeoutMs = 60_000; // 1min

    @JsonProperty
    private String cachedResourcesFile = null;

    @JsonProperty
    private String cachedResourcesBackupFile = null;

    // ----------------------------------------------------------------------

    @JsonProperty("centreRegistryUrl")
    public String getCentreRegistryUrl() {
        return centreRegistryUrl;
    }

    @JsonProperty("maxScanDepth")
    public int getMaxScanDepth() {
        return maxScanDepth;
    }

    @JsonProperty("scanTaskInitialDelay")
    public long getScanTaskInitialDelay() {
        return scanTaskInitialDelay;
    }

    @JsonProperty("scanTaskInterval")
    public long getScanTaskInterval() {
        return scanTaskInterval;
    }

    @JsonProperty("scanTaskIntervalTimeUnit")
    public TimeUnit getScanTaskIntervalTimeUnit() {
        return scanTaskIntervalTimeUnit;
    }

    @JsonProperty("maxConcurrentRequests")
    public int getMaxConcurrentRequests() {
        return maxConcurrentRequests;
    }

    @JsonProperty("requestTimeoutMs")
    public int getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    @JsonProperty("cachedResourcesFile")
    public String getCachedResourcesFile() {
        return cachedResourcesFile;
    }

    @JsonProperty("cachedResourcesBackupFile")
    public String getCachedResourcesBackupFile() {
        return cachedResourcesBackupFile;
    }

}
