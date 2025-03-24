package eu.clarin.sru.fcs.aggregator.client;

import eu.clarin.sru.client.SRUCallback;
import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUSearchRetrieveRequest;
import eu.clarin.sru.client.SRUSearchRetrieveResponse;
import eu.clarin.sru.client.SRUThreadedClient;

/**
 * @author edima
 */
class SearchOperation implements Operation<SRUSearchRetrieveRequest, SRUSearchRetrieveResponse>,
        SRUCallback<SRUSearchRetrieveRequest, SRUSearchRetrieveResponse> {
    SRUSearchRetrieveRequest request;
    ThrottledClient.SearchCallback callback;
    GenericClient client;
    OperationStats stats;

    public SearchOperation(SRUSearchRetrieveRequest request, ThrottledClient.SearchCallback callback) {
        this.request = request;
        this.callback = callback;
        this.stats = new OperationStats();
    }

    @Override
    public void setClient(GenericClient client) {
        this.client = client;
    }

    @Override
    public void execute(SRUThreadedClient sruClient) {
        try {
            sruClient.searchRetrieve(request, this);
        } catch (SRUClientException xc) {
            callback.onError(request, xc, stats);
        }
    }

    @Override
    public void onSuccess(SRUSearchRetrieveResponse response) {
        try {
            stats.finishedTime = System.currentTimeMillis();
            callback.onSuccess(response, stats);
        } finally {
            client.executeNextOperationOfEndpoint(request.getBaseURI());
        }
    }

    @Override
    public void onError(SRUSearchRetrieveRequest request, SRUClientException error) {
        try {
            stats.finishedTime = System.currentTimeMillis();
            callback.onError(request, error, stats);
        } finally {
            client.executeNextOperationOfEndpoint(request.getBaseURI());
        }
    }

    @Override
    public OperationStats stats() {
        return stats;
    }
}
