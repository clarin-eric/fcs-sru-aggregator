package eu.clarin.sru.fcs.aggregator.data;

import eu.clarin.sru.client.SRUClient;
import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUScanRequest;
import eu.clarin.sru.client.SRUScanResponse;
import eu.clarin.sru.client.SRUTerm;
import eu.clarin.sru.client.SRUVersion;
import eu.clarin.sru.fcs.aggregator.sparam.CorpusTreeNode;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Endpoint node. Can have Corpus children.
 * 
 * @author Yana Panchenko
 */
public class Endpoint implements CorpusTreeNode {

    private String url;
    private Institution institution;
    private List<Corpus> corpora;
    private boolean hasChildrenLoaded = false;

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

    private List<Corpus> getCorpora() {
        if (!this.hasChildrenLoaded) {
            loadChildren();
        }
        return corpora;
    }

    @Override
    public boolean hasChildrenLoaded() {
        return this.hasChildrenLoaded;
    }

    @Override
    public void loadChildren() {
        if (hasChildrenLoaded) {
            return;
        }
        this.hasChildrenLoaded = true;
        //this.corpora = EndpointY.getCorpora(url);
        loadChildCorpora();
    }

    @Override
    public List<Corpus> getChildren() {
        loadChildren();
        return this.corpora;
    }

    @Override
    public CorpusTreeNode getChild(int index) {
        loadChildren();
        if (index >= corpora.size()) {
            return null;
        }
        return corpora.get(index);
    }

//    public static ArrayList<CorpusY> getCorpora(String endpointUrl) {
//        return getCorpora(endpointUrl, ""); //TODO to be changed to 'root'
//    }
//
//    public static ArrayList<CorpusY> getCorpora(String endpointUrl, String resourceHandler) {
//
//        ArrayList<CorpusY> corpora = new ArrayList<CorpusY>();
//        SRUScanResponse corporaResponse = null;
//        StringBuilder scanClause = new StringBuilder("fcs.resource");
//        if (resourceHandler.length() > 0) {
//            scanClause.append("=");
//        }
//        scanClause.append(resourceHandler);
//        try {
//            SRUClient sruClient = new SRUClient(SRUVersion.VERSION_1_2);
//            SRUScanRequest corporaRequest = new SRUScanRequest(endpointUrl);
//            corporaRequest.setScanClause(scanClause.toString());
//            //TODO extra data?
//            //corporaRequest.setExtraRequestData("x-cmd-resource-info", "true");
//            corporaResponse = sruClient.scan(corporaRequest);
//        } catch (SRUClientException ex) {
//            Logger.getLogger(EndpointY.class.getName()).log(Level.SEVERE, "Error accessing corpora at " + endpointUrl + " for " + resourceHandler, ex);
//        }
//        if (corporaResponse != null && corporaResponse.hasTerms()) {
//            for (SRUTerm term : corporaResponse.getTerms()) {
//                CorpusY c = new CorpusY(endpointUrl);
//                c.setValue(term.getValue());
//                c.setDisplayTerm(term.getDisplayTerm());
//                c.setNumberOfRecords(term.getNumberOfRecords());
//                corpora.add(c);
//            }
//        }
//        return corpora;
//
//    }
    

    private void loadChildCorpora() {

        corpora = new ArrayList<Corpus>();
        SRUScanResponse corporaResponse = null;
        StringBuilder scanClause = new StringBuilder("fcs.resource");
        try {
            SRUClient sruClient = new SRUClient(SRUVersion.VERSION_1_2);
            SRUScanRequest corporaRequest = new SRUScanRequest(url);
            corporaRequest.setScanClause(scanClause.toString());
            //TODO extra data?
            //corporaRequest.setExtraRequestData("x-cmd-resource-info", "true");
            corporaResponse = sruClient.scan(corporaRequest);
        } catch (SRUClientException ex) {
            Logger.getLogger(Endpoint.class.getName()).log(Level.SEVERE, "Error accessing corpora at " + url, ex);
        }
        if (corporaResponse != null && corporaResponse.hasTerms()) {
            for (SRUTerm term : corporaResponse.getTerms()) {
                Corpus c = new Corpus(this);
                c.setValue(term.getValue());
                c.setDisplayTerm(term.getDisplayTerm());
                c.setNumberOfRecords(term.getNumberOfRecords());
                corpora.add(c);
            }
        }
    }
    
    @Override
    public String toString() {
        return url;
    }
}
