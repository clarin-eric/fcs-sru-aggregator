package eu.clarin.sru.fcs.aggregator.core;

import java.util.concurrent.TimeUnit;

public interface ScanCrawlSchedulerParams {
    default long getScanTaskInitialDelay() {
        return 0; // no delay, see getScanTaskTimeUnit
    }

    default long getScanTaskInterval() {
        return 12; // see getScanTaskTimeUnit
    }

    default TimeUnit getScanTaskTimeUnit() {
        return TimeUnit.HOURS;
    }
}
