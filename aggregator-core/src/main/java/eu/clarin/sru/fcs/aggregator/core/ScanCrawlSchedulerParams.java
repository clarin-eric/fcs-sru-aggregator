package eu.clarin.sru.fcs.aggregator.core;

import java.util.concurrent.TimeUnit;

public interface ScanCrawlSchedulerParams {
    long getScanTaskInitialDelay();

    long getScanTaskInterval();

    TimeUnit getScanTaskTimeUnit();
}
