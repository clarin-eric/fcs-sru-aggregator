package eu.clarin.sru.fcs.aggregator.cache;

import eu.clarin.sru.fcs.aggregator.sopt.Endpoint;

/**
 * Filter for the cache of scan data (endpoint/resources descriptions) - for
 * specifying if only particular endpoints have not to be cached. Useful for
 * testing the endpoints.
 * 
 * @author yanapanchenko
 */
public interface EndpointFilter {

    Iterable<Endpoint> filter(Iterable<Endpoint> endpoints);
    
}
