package eu.clarin.sru.fcs.aggregator.registry;

import java.util.*;

/**
 * Institution. Contains information about institution name and link (url). 
 * Can have information about its CQL Endpoints.
 * 
 * @author Yana Panchenko
 */
    public class Institution {

    private String name;
    private String link;
    private ArrayList<Endpoint> endpoints;

    public Institution(String name, String link) {
        this.name = name;
        this.link = link;
		this.endpoints = new ArrayList<Endpoint>();
    }
    
    public Endpoint add(String endpointUrl) {
        Endpoint ep = getEndpoint(endpointUrl);
        if (ep == null) {
            ep = new Endpoint(endpointUrl, this);
            endpoints.add(ep);
        }
        return ep;
    }

    public String getName() {
        return name;
    }
    
    public String getLink() {
        return link;
    }
    
   public List<Endpoint> getEndpoints() {
        return  this.endpoints;
    }


    public Endpoint getEndpoint(int index) {
        if (index >= endpoints.size()) {
            return null;
        }
        return endpoints.get(index);
    }

    public Endpoint getEndpoint(String endpointUrl) {
        for (Endpoint ep : endpoints) {
            if (ep.getUrl().equals(endpointUrl)) {
                return ep;
            }
        }
        return null;
    }
    
    @Override
    public String toString() {
        if (name != null && name.length() > 0) {
            return name;
        } else {
        return link;
       }
    }
    
}
