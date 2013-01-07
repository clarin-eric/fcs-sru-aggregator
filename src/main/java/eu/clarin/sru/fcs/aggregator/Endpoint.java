package eu.clarin.sru.fcs.aggregator;

import java.util.*;

public class Endpoint {
    private String url;
    private String institution;
    private ArrayList<String> corpora;

    public Endpoint(String url, String institution) {
        this.url = url;
        this.institution = institution;
        this.corpora = new ArrayList<String>();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public ArrayList<String> getCorpora() {
        return corpora;
    }

    public void setCorpora(ArrayList<String> corpora) {
        this.corpora = corpora;
    }
    
    
}
