package eu.clarin.sru.fcs.aggregator.core;

public interface AggregatorParams
        extends SRUFCSClientParams, ScanCrawlParams, ShutdownParams, SearchGCParams, FCSAuthenticationParams {
    // aggregates params interfaces

    boolean enableScanCrawlTask();
}
