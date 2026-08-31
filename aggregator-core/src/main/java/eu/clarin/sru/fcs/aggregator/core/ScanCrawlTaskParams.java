package eu.clarin.sru.fcs.aggregator.core;

public interface ScanCrawlTaskParams extends EndpointOverrideParams {
    String getCenterRegistryUrl();

    default int getScanMaxDepth() {
        return 3;
    }
}
