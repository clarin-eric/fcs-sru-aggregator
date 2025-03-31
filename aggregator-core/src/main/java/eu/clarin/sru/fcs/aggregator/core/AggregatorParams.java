package eu.clarin.sru.fcs.aggregator.core;

public interface AggregatorParams extends SRUFCSClientParams, ScanCrawlParams, ShutdownParams, SearchGCParams {
    // aggregates params interfaces

    boolean enableScanCrawlTask();
}
