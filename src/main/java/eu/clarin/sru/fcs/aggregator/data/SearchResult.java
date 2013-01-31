/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.clarin.sru.fcs.aggregator.data;

import eu.clarin.sru.client.SRUSearchRetrieveResponse;
import eu.clarin.sru.client.fcs.DataViewKWIC;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the results of a SRU search-retrieve operation request. It
 * contains the endpoint and corpus (if specified in the request) to which a
 * request was sent, and the corresponding SRU search-retrieve response.
 *
 * @author Yana Panchenko
 */
public class SearchResult {

    private Endpoint endpoint;
    private Corpus corpus;
    private Future<SRUSearchRetrieveResponse> futureResponse;
    private SRUSearchRetrieveResponse response;
    private List<DataViewKWIC> dataKWIC = new ArrayList<DataViewKWIC>();
    
    private static final Logger logger = Logger.getLogger(SearchResult.class.getName());

    public List<DataViewKWIC> getDataKWIC() {
        return dataKWIC;
    }

    public void addKWIC(DataViewKWIC kw) {
        this.dataKWIC.add(kw);
    }
    

    public SearchResult(Object nodeData) {
        if (nodeData instanceof Endpoint) {
            endpoint = (Endpoint) nodeData;
        } else {
            corpus = (Corpus) nodeData;
            endpoint = corpus.getEndpoint();
        }
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public Corpus getCorpus() {
        return corpus;
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
        if (corpus != null && corpus.getValue() != null) {
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
            logger.log(Level.SEVERE, "Error consuming response from {0} {1}\n {2}\n {3}", 
                    new Object[]{endpoint.getUrl(), corpus, ex.getClass().getName(), ex.getMessage()});
        }
    }
}
