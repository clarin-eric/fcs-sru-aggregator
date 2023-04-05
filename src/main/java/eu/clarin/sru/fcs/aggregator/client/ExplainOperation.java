package eu.clarin.sru.fcs.aggregator.client;

import eu.clarin.sru.client.SRUCallback;
import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUExplainRequest;
import eu.clarin.sru.client.SRUExplainResponse;
import eu.clarin.sru.client.SRUThreadedClient;

/**
 *
 * @author edima
 */
class ExplainOperation implements Operation<SRUExplainRequest, SRUExplainResponse>,
        SRUCallback<SRUExplainRequest, SRUExplainResponse> {
    SRUExplainRequest request;
    ThrottledClient.ExplainCallback callback;
    GenericClient client;
    OpStats stats;

    public ExplainOperation(SRUExplainRequest request, ThrottledClient.ExplainCallback callback) {
        this.request = request;
        this.callback = callback;
        this.stats = new OpStats();
    }

    @Override
    public void setClient(GenericClient client) {
        this.client = client;
    }

    @Override
    public void execute(SRUThreadedClient sruClient) {
        try {
            sruClient.explain(request, this);
        } catch (SRUClientException xc) {
            onError(request, xc);
        }
    }

    @Override
    public void onSuccess(SRUExplainResponse response) {
        try {
            stats.finishedTime = System.currentTimeMillis();
            callback.onSuccess(response, stats);
        } finally {
            client.executeNextOperationOfEndpoint(request.getBaseURI());
        }
    }

    @Override
    public void onError(SRUExplainRequest request, SRUClientException error) {
        try {
            stats.finishedTime = System.currentTimeMillis();
            callback.onError(request, error, stats);
        } finally {
            client.executeNextOperationOfEndpoint(request.getBaseURI());
        }
    }

    @Override
    public OpStats stats() {
        return stats;
    }
}
