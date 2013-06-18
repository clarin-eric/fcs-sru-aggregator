package eu.clarin.sru.fcs.aggregator.sopt;

/**
 * Endpoint. Contains the parent Institution.
 * 
 * @author Yana Panchenko
 */
public class Endpoint {

    private String url;
    private InstitutionI institution;

    public Endpoint(String url, InstitutionI institution) {
        this.url = url;
        this.institution = institution;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public InstitutionI getInstitution() {
        return institution;
    }

    public void setInstitution(InstitutionI institution) {
        this.institution = institution;
    }

    @Override
    public String toString() {
        return url;
    }
}
