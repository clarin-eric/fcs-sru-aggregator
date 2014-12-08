package eu.clarin.sru.fcs.aggregator.client;

import eu.clarin.sru.client.SRUCallback;
import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUScanRequest;
import eu.clarin.sru.client.SRUScanResponse;
import eu.clarin.sru.client.SRUThreadedClient;

/**
 *
 * @author edima
 */
class ScanOperation implements Operation<SRUScanRequest, SRUScanResponse>, SRUCallback<SRUScanRequest, SRUScanResponse> {
	SRUScanRequest request;
	ThrottledClient.ScanCallback callback;
	GenericClient client;
	OpStats stats;

	public ScanOperation(SRUScanRequest request, ThrottledClient.ScanCallback callback) {
		this.request = request;
		this.callback = callback;
	}

	@Override
	public void setClient(GenericClient client) {
		this.client = client;
	}

	@Override
	public void execute(SRUThreadedClient sruClient) {
		try {
			sruClient.scan(request, this);
		} catch (SRUClientException xc) {
			stats.finishedTime = System.currentTimeMillis();
			callback.onError(request, xc, stats);
		}
	}

	@Override
	public void onSuccess(SRUScanResponse response) {
		try {
			stats.finishedTime = System.currentTimeMillis();
			callback.onSuccess(response, stats);
		} finally {
			client.executeNextOperationOfEndpoint(request.getBaseURI());
		}
	}

	@Override
	public void onError(SRUScanRequest request, SRUClientException error) {
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
