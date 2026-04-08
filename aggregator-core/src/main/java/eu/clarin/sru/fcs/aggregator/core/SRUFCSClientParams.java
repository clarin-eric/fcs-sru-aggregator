package eu.clarin.sru.fcs.aggregator.core;

public interface SRUFCSClientParams extends EndpointOverrideParams {
    int getEndpointScanTimeout();

    int getEndpointSearchTimeout();

    int getMaxConcurrentScanRequestsPerEndpoint();

    int getMaxConcurrentSearchRequestsPerEndpoint();
}
