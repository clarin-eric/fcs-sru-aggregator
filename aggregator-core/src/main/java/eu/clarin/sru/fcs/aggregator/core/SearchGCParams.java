package eu.clarin.sru.fcs.aggregator.core;

public interface SearchGCParams {
    default int getSearchesSizeThreshold() {
        return 1_000; // 1k searches
    }

    default long getSearchesAgeThreshold() {
        return 3600_000; // 60min
    }
}
