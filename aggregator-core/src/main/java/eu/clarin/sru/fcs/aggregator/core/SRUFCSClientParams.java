package eu.clarin.sru.fcs.aggregator.core;

import java.net.URI;
import java.util.List;

public interface SRUFCSClientParams {
    int getEndpointScanTimeout();

    int getEndpointSearchTimeout();

    int getMaxConcurrentScanRequestsPerEndpoint();

    int getMaxConcurrentSearchRequestsPerEndpoint();

    int getMaxConcurrentSearchRequestsPerSlowEndpoint();

    List<URI> getSlowEndpoints();
}
