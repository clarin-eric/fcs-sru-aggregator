package eu.clarin.sru.fcs.aggregator.rest;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.core.Application;

/**
 * RESTful service. At the moment does nothing useful and was added just to
 * make sure that it would be possible to combine REST services with ZK app,
 * and add to the aggregator the support for its usage as aggregated FCS
 * server, as was planned in the initial FCS specification.
 * 
 * @author yanapanchenko
 */
public class AggregatorService  extends Application {
	
    private Set<Object> singletons;
	
    @Override
    public Set<Class<?>> getClasses() {
        return Collections.emptySet();
    }
    
       @Override
    public Set<Object> getSingletons() {
        if (singletons == null) {
            AggregatedEndpoint aggregatedEp = new AggregatedEndpoint();
            singletons = new HashSet<Object>(1);
            singletons.add(aggregatedEp);
        }
        return singletons;
    }
}
