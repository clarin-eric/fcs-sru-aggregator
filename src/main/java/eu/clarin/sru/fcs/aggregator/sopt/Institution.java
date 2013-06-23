package eu.clarin.sru.fcs.aggregator.sopt;

import java.util.*;
import java.util.logging.Logger;

/**
 * Institution. Can have Endpoint children.
 * 
 * @author Yana Panchenko
 */
    public class Institution implements InstitutionI {

    private String name;
    private String link;
    private ArrayList<Endpoint> endpoints;

    public Institution(String name, String link) {
        this.name = name;
        this.link = link;
        this.endpoints = new ArrayList<Endpoint>();
    }
    
    @Override
    public Endpoint add(String endpointUrl) {
        Endpoint ep = new Endpoint(endpointUrl, this);
        endpoints.add(ep);
        return ep;
    }

    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getLink() {
        return link;
    }
    
    @Override
   public List<Endpoint> getEndpoints() {
        return  this.endpoints;
    }
   
    
    @Override
    public Endpoint getEndpoint(int index) {
        if (index >= endpoints.size()) {
            return null;
        }
        return endpoints.get(index);
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
