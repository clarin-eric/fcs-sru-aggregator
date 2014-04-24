package eu.clarin.sru.fcs.aggregator.rest;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.core.Application;

/**
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
