package eu.clarin.sru.fcs.aggregator.core;

public interface ScanCrawlTaskParams extends EndpointOverrideParams {
    String getCenterRegistryUrl();

    int getScanMaxDepth();
}
