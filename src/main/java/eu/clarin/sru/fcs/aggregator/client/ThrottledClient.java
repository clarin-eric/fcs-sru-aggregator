package eu.clarin.sru.fcs.aggregator.client;

import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUExplainRequest;
import eu.clarin.sru.client.SRUExplainResponse;
import eu.clarin.sru.client.SRUScanRequest;
import eu.clarin.sru.client.SRUScanResponse;
import eu.clarin.sru.client.SRUSearchRetrieveRequest;
import eu.clarin.sru.client.SRUSearchRetrieveResponse;
import eu.clarin.sru.client.SRUThreadedClient;

/**
 * @author edima
 */
public class ThrottledClient {

	public interface Stats {

		long getQueueTime();

		long getExecutionTime();
	}

	public interface ExplainCallback {

		void onSuccess(SRUExplainResponse response, Stats stats);

		void onError(SRUExplainRequest request, SRUClientException error, Stats stats);
	}

	public interface ScanCallback {

		void onSuccess(SRUScanResponse response, Stats stats);

		void onError(SRUScanRequest request, SRUClientException error, Stats stats);
	}

	public interface SearchCallback {

		void onSuccess(SRUSearchRetrieveResponse response, Stats stats);

		void onError(SRUSearchRetrieveRequest request, SRUClientException error, Stats stats);
	}

	GenericClient client;

	public ThrottledClient(SRUThreadedClient sruClient, int maxConcurrentRequests) {
		this.client = new GenericClient(sruClient, maxConcurrentRequests);
	}

	public void explain(SRUExplainRequest request, ExplainCallback callback) {
		client.execute(request.getBaseURI(), new ExplainOperation(request, callback));
	}

	public void scan(SRUScanRequest request, ScanCallback callback) {
		client.execute(request.getBaseURI(), new ScanOperation(request, callback));
	}

	public void searchRetrieve(SRUSearchRetrieveRequest request, SearchCallback callback) {
		client.execute(request.getBaseURI(), new SearchOperation(request, callback));
	}

	public void shutdown() {
		client.shutdown();
	}

	public void shutdownNow() {
		client.shutdownNow();
	}
}
