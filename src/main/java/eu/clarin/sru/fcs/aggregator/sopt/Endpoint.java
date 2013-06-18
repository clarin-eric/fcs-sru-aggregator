package eu.clarin.sru.fcs.aggregator.sopt;

/**
 * Endpoint. Contains the parent Institution.
 * 
 * @author Yana Panchenko
 */
public class Endpoint {

    private String url;
    private Institution institution;

    public Endpoint(String url, Institution institution) {
        this.url = url;
        this.institution = institution;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    @Override
    public String toString() {
        return url;
    }
}
