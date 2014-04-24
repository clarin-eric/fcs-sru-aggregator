package eu.clarin.sru.fcs.aggregator.cache;

import eu.clarin.sru.fcs.aggregator.sopt.Endpoint;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author yanapanchenko
 */
public class EndpointUrlFilter implements EndpointFilter {
    
    private String[] urlShouldContain = new String[0];
    
    public void urlShouldContainAnyOf(String ... urlSubstrings) {
        urlShouldContain = urlSubstrings;
    }

    @Override
    public Iterable<Endpoint> filter(Iterable<Endpoint> endpoints) {
        List<Endpoint> filtered = new ArrayList<Endpoint>();
        
        for (Endpoint endp : endpoints) {
            for (String urlSubstring : urlShouldContain) {
                if (endp.getUrl().contains(urlSubstring)) {
                    filtered.add(endp);
                    break;
                }
            }
        }
        
        return filtered;
    }
    
}
