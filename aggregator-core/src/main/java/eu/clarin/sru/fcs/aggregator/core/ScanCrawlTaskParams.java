package eu.clarin.sru.fcs.aggregator.core;

import java.util.List;

import eu.clarin.sru.fcs.aggregator.scan.EndpointConfig;

public interface ScanCrawlTaskParams {
    String getCenterRegistryUrl();

    int getScanMaxDepth();

    List<EndpointConfig> getAdditionalCQLEndpoints();

    List<EndpointConfig> getAdditionalFCSEndpoints();
}
