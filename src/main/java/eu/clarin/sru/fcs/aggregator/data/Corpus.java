package eu.clarin.sru.fcs.aggregator.data;

import eu.clarin.sru.client.SRUClient;
import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUScanRequest;
import eu.clarin.sru.client.SRUScanResponse;
import eu.clarin.sru.client.SRUTerm;
import eu.clarin.sru.client.SRUVersion;
import eu.clarin.sru.fcs.aggregator.sparam.CorpusTreeNode;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Corpus node. Can have Corpus children, i.e. sub-corpora.
 * 
 * @author Yana Panchenko
 */
public class Corpus implements CorpusTreeNode {
    private String value;
    private Integer numberOfRecords;
    private String displayTerm;
    private String lang;
    private List<Corpus> subCorpora = new ArrayList<Corpus>();
    private boolean hasChildrenLoaded = false;
    private Endpoint endpoint;
    
    private static final Logger logger = Logger.getLogger(Corpus.class.getName());
    
    public Corpus(Endpoint endpoint) {
        this.value = null;
        this.numberOfRecords = null;
        this.displayTerm = null;
        this.lang = null;
        this.endpoint = endpoint;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Integer getNumberOfRecords() {
        return numberOfRecords;
    }

    public void setNumberOfRecords(Integer numberOfRecords) {
        this.numberOfRecords = numberOfRecords;
    }

    public String getDisplayTerm() {
        return displayTerm;
    }

    public void setDisplayTerm(String displayTerm) {
        this.displayTerm = displayTerm;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }
    
//    public String getEndpointUrl() {
//        return this.endpointUrl;
//    }
    
    public Endpoint getEndpoint() {
        return this.endpoint;
    }

    @Override
    public boolean hasChildrenLoaded() {
        return this.hasChildrenLoaded;
    }

    @Override
    public void loadChildren() {
        if (this.hasChildrenLoaded) {
            return;
        }
        this.hasChildrenLoaded = true;
        //this.subCorpora = EndpointY.getCorpora(this.endpointUrl, value);
        loadChildCorpora();
    }

    @Override
    public List<Corpus> getChildren() {
         return getSubCorpora();
    }
    
    private List<Corpus> getSubCorpora() {
        loadChildren();
        return subCorpora;
    }

    @Override
    public CorpusTreeNode getChild(int index) {
        loadChildren();
        if (index >= subCorpora.size()) {
            return null;
        }
        return subCorpora.get(index);
    }
    
    @Override
    public String toString() {
        if (displayTerm != null && displayTerm.length() > 0) {
            return displayTerm;
        } else {
            return value;
        }
   }
    
   private void loadChildCorpora() {

        subCorpora = new ArrayList<Corpus>();
        SRUScanResponse corporaResponse = null;
        StringBuilder scanClause = new StringBuilder("fcs.resource");
        scanClause.append("=");
        scanClause.append("");
        scanClause.append(value);
        scanClause.append("");
        try {
            SRUClient sruClient = new SRUClient(SRUVersion.VERSION_1_2);
            SRUScanRequest corporaRequest = new SRUScanRequest(this.endpoint.getUrl());
            corporaRequest.setScanClause(scanClause.toString());
            //TODO extra data?
            //corporaRequest.setExtraRequestData("x-cmd-resource-info", "true");
            corporaResponse = sruClient.scan(corporaRequest);
        } catch (SRUClientException ex) {
            logger.log(Level.SEVERE, "Error accessing corpora {0} at {1} {2} {3}", 
                    new String[]{value, endpoint.getUrl(), ex.getClass().getName(), ex.getMessage()});
        }
        if (corporaResponse != null && corporaResponse.hasTerms()) {
            for (SRUTerm term : corporaResponse.getTerms()) {
                Corpus c = new Corpus(this.endpoint);
                c.setValue(term.getValue());
                c.setDisplayTerm(term.getDisplayTerm());
                c.setNumberOfRecords(term.getNumberOfRecords());
                subCorpora.add(c);
            }
        }
    }
   
}