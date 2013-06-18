package eu.clarin.sru.fcs.aggregator.sopt;

import java.util.*;

/**
 * Institution used for testing. The endpoints to be tested are provided by the
 * centers that want to test their endpoints before putting the in the center 
 * registry and on the productive server. The provided endpoints' url are hard-coded here.
 * 
 * @author Yana Panchenko
 */
public class InstitutionForTesting extends Institution {

    private List<Endpoint> endpoints = new ArrayList<Endpoint>();
    private boolean hasChildrenLoaded = false;
    private String[] endpointUrls;
    

    public InstitutionForTesting(String name, String url, String[] endpointUrls) {
        super(name, url);
        this.endpointUrls = endpointUrls;
    }

    @Override
    public boolean hasChildrenLoaded() {
        return hasChildrenLoaded;
    }

    @Override
    public void loadChildren() {
        if (hasChildrenLoaded) {
            return;
        }
        hasChildrenLoaded = true;
        for (int j = 0; j < endpointUrls.length; j++) {
                String epUrl = endpointUrls[j];
                Endpoint endpoint = new Endpoint(epUrl, this);
                endpoints.add(endpoint);
        } 
    }
    
    @Override
    public List<Endpoint> getChildren() {
        loadChildren();
        return  this.endpoints;
    }
    
    @Override
    public Endpoint getChild(int index) {
        loadChildren();
        if (index >= endpoints.size()) {
            return null;
        }
        return endpoints.get(index);
    }
}
