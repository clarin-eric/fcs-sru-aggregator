package eu.clarin.sru.fcs.aggregator.client;

import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUScanRequest;
import eu.clarin.sru.client.SRUScanResponse;
import eu.clarin.sru.client.SRUSearchRetrieveRequest;
import eu.clarin.sru.client.SRUSearchRetrieveResponse;
import eu.clarin.sru.client.SRUThreadedClient;

/**
 * @author edima
 */
public class ThrottledClient {

	public final static int MAX_CONCURRENT_REQUESTS = 8;

	public interface Stats {

		long getQueueTime();

		long getExecutionTime();
	}

	public interface ScanCallback {

		void onSuccess(SRUScanResponse response, Stats stats);

		void onError(SRUScanRequest request, SRUClientException error, Stats stats);
	}

	public interface SearchCallback {

		void onSuccess(SRUSearchRetrieveResponse response, Stats stats);

		void onError(SRUSearchRetrieveRequest request, SRUClientException error, Stats stats);
	}

	SRUThreadedClient sruClient;
	GenericClient scanClient;
	GenericClient searchClient;

	public ThrottledClient(SRUThreadedClient sruClient) {
		this.sruClient = sruClient;
		this.scanClient = new GenericClient(sruClient);
		this.searchClient = new GenericClient(sruClient);
	}

	public void scan(SRUScanRequest request, ScanCallback callback) {
		scanClient.execute(request.getBaseURI(), new ScanOperation(request, callback));
	}

	public void searchRetrieve(SRUSearchRetrieveRequest request, SearchCallback callback) {
		searchClient.execute(request.getBaseURI(), new SearchOperation(request, callback));
	}

	public void shutdown() {
		sruClient.shutdown();
	}

	public void shutdownNow() {
		sruClient.shutdownNow();
	}
}
