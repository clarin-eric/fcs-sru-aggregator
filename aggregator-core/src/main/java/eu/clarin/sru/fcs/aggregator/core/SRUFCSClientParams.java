package eu.clarin.sru.fcs.aggregator.core;

public interface SRUFCSClientParams extends EndpointOverrideParams {
    default int getEndpointScanTimeout() {
        return 60_000; // 1min
    }

    default int getEndpointSearchTimeout() {
        return 30_000; // 30sec
    }

    default int getMaxConcurrentScanRequestsPerEndpoint() {
        return 4;
    }

    default int getMaxConcurrentSearchRequestsPerEndpoint() {
        return 4;
    }
}
