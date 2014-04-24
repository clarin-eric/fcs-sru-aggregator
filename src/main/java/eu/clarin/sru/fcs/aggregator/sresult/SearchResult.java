package eu.clarin.sru.fcs.aggregator.sresult;

import eu.clarin.sru.fcs.aggregator.sopt.Corpus;
import eu.clarin.sru.client.SRUSearchRetrieveResponse;
import eu.clarin.sru.client.fcs.DataViewKWIC;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the results of a SRU search-retrieve operation request. It
 * contains the endpoint/corpus (if specified in the request) to which a
 * request was sent, and the corresponding SRU search-retrieve response.
 *
 * @author Yana Panchenko
 */
public class SearchResult {

    private Corpus corpus;
    private String searchString;
    private int startRecord;
    private int endRecord;
    private Future<SRUSearchRetrieveResponse> futureResponse;
    private SRUSearchRetrieveResponse response;
    private List<Kwic> kwics = new ArrayList<Kwic>();
    
    private static final Logger LOGGER = Logger.getLogger(SearchResult.class.getName());

    public List<Kwic> getKwics() {
        return kwics;
    }

    public void addKwic(DataViewKWIC kw, String pid, String reference) {
        Kwic kwic = new Kwic(kw, pid, reference);
        this.kwics.add(kwic);
    }
    
    public SearchResult(Corpus corpus, String searchString, int startRecord , int endRecord) {
        this.corpus = corpus;
        this.searchString = searchString;
        this.startRecord = startRecord;
        this.endRecord = endRecord;
    }

    public int getStartRecord() {
        return startRecord;
    }

    public int getEndRecord() {
        return endRecord;
    }

    public Corpus getCorpus() {
        return corpus;
    }
    
    public String getSearchString() {
        return searchString;
    }

    public void setFutureResponse(Future<SRUSearchRetrieveResponse> futureResponse) {
        this.futureResponse = futureResponse;
    }

    public void setResponse(SRUSearchRetrieveResponse response) {
        this.response = response;
    }

    public SRUSearchRetrieveResponse getResponse() {
        return response;
    }

    public boolean hasCorpusHandler() {
        if (corpus != null && corpus.getHandle() != null) {
            return true;
        }
        return false;
    }

    public void cancelWaitingForResponse() {
        futureResponse.cancel(true);
    }

    public boolean isWaitingForResponse() {
        if (futureResponse == null) {
            return false;
        } else if (futureResponse.isDone()) {
            return false;
        } else {
            return true;
        }
    }

    public void consumeResponse() {
        try {
            if (futureResponse != null) {
                response = futureResponse.get();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error consuming response from {0} {1} {2} {3}", 
                    new Object[]{corpus.getEndpointUrl(), corpus, ex.getClass().getName(), ex.getMessage()});
        }
    }
}
