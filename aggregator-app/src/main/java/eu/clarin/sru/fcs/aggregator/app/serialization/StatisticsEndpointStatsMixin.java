package eu.clarin.sru.fcs.aggregator.app.serialization;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import eu.clarin.sru.fcs.aggregator.scan.FCSProtocolVersion;
import eu.clarin.sru.fcs.aggregator.scan.FCSSearchCapabilities;
import eu.clarin.sru.fcs.aggregator.scan.Statistics.EndpointStats.DiagPair;
import eu.clarin.sru.fcs.aggregator.scan.Statistics.EndpointStats.ExcPair;

public abstract class StatisticsEndpointStatsMixin {
    @JsonProperty
    FCSProtocolVersion version;

    @JsonProperty
    EnumSet<FCSSearchCapabilities> searchCapabilities;

    @JsonProperty
    List<String> rootResources;

    @JsonProperty
    int maxConcurrentRequests;

    @JsonProperty
    Map<String, DiagPair> diagnostics;

    @JsonProperty
    Map<String, ExcPair> errors;

    @JsonProperty
    abstract double getAvgQueueTime();

    @JsonProperty
    abstract double getMaxQueueTime();

    @JsonProperty
    abstract double getAvgExecutionTime();

    @JsonProperty
    abstract double getMaxExecutionTime();

    @JsonProperty
    abstract int getNumberOfRequests();
}
