package eu.clarin.sru.fcs.aggregator;

import java.util.*;

public class Institution {
    private String title;
    private ArrayList<Endpoint> endpoints;

    public Institution(String title, String institution) {
        this.title = title;
        this.endpoints = new ArrayList<Endpoint>();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ArrayList<Endpoint> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(ArrayList<Endpoint> endpoints) {
        this.endpoints = endpoints;
    }
    
    
    
}
