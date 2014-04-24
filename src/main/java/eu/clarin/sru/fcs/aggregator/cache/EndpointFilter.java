package eu.clarin.sru.fcs.aggregator.cache;

import eu.clarin.sru.fcs.aggregator.sopt.Endpoint;

/**
 *
 * @author yanapanchenko
 */
public interface EndpointFilter {

    Iterable<Endpoint> filter(Iterable<Endpoint> endpoints);
    
}
