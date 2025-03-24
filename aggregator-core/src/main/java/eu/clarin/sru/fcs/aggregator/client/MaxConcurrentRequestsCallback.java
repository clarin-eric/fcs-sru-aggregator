package eu.clarin.sru.fcs.aggregator.client;

import java.net.URI;

/**
 * @author edima
 */
public interface MaxConcurrentRequestsCallback {

    int getMaxConcurrentRequest(URI baseURI);

}
