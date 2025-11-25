package eu.clarin.sru.fcs.aggregator.client;

import eu.clarin.sru.client.CancellableSRUCallback;
import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUSearchRetrieveRequest;
import eu.clarin.sru.client.SRUSearchRetrieveResponse;
import eu.clarin.sru.client.SRUThreadedClient;

/**
 * @author edima
 * @author ekoerner
 */
class SearchOperation implements CancellableOperation<SRUSearchRetrieveRequest, SRUSearchRetrieveResponse>,
        CancellableSRUCallback<SRUSearchRetrieveRequest, SRUSearchRetrieveResponse> {

    SRUSearchRetrieveRequest request;
    ThrottledClient.SearchCallback callback;
    GenericClient client;
    OperationStats stats;
    boolean cancelled;

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
        if (cancelled) {
            onCancelled(request);
            return;
        }

        try {
            sruClient.searchRetrieve(request, this);
        } catch (SRUClientException xc) {
            callback.onError(request, xc, stats);
        }
    }

    @Override
    public void cancel() {
        if (stats.finishedTime == 0) {
            // only change state if not already finished
            cancelled = true;
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
    public void onCancelled(SRUSearchRetrieveRequest request) {
        try {
            stats.finishedTime = System.currentTimeMillis();
            callback.onCancelled(request, stats);
        } finally {
            client.executeNextOperationOfEndpoint(request.getBaseURI());
        }
    }

    @Override
    public OperationStats stats() {
        return stats;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

} // class SearchOperation
