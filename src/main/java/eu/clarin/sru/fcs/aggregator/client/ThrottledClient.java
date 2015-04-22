package eu.clarin.sru.fcs.aggregator.client;

import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUExplainRequest;
import eu.clarin.sru.client.SRUExplainResponse;
import eu.clarin.sru.client.SRUScanRequest;
import eu.clarin.sru.client.SRUScanResponse;
import eu.clarin.sru.client.SRUSearchRetrieveRequest;
import eu.clarin.sru.client.SRUSearchRetrieveResponse;
import eu.clarin.sru.client.SRUThreadedClient;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author edima
 *
 * This is a SRUThreadedClient wrapper that only allows a limited number of
 * requests per endpoint, depending on the requests type. To determine the
 * number of requests for each endpoint the user must provide a callback.
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

	GenericClient scanClient;
	GenericClient searchClient;

	public ThrottledClient(
			SRUThreadedClient sruScanClient, MaxConcurrentRequestsCallback scanConcurrentCallback,
			SRUThreadedClient sruSearchClient, MaxConcurrentRequestsCallback searchConcurrentCallback) {
		this.scanClient = new GenericClient(sruScanClient, scanConcurrentCallback);
		this.searchClient = new GenericClient(sruSearchClient, searchConcurrentCallback);
	}

	public void explain(SRUExplainRequest request, ExplainCallback callback) {
		scanClient.execute(request.getBaseURI(), new ExplainOperation(request, callback));
	}

	public void scan(SRUScanRequest request, ScanCallback callback) {
		scanClient.execute(request.getBaseURI(), new ScanOperation(request, callback));
	}

	public void searchRetrieve(SRUSearchRetrieveRequest request, SearchCallback callback) {
		searchClient.execute(request.getBaseURI(), new SearchOperation(request, callback));
	}

	public void shutdown() {
		scanClient.shutdown();
		searchClient.shutdown();
	}

	public void shutdownNow() {
		scanClient.shutdownNow();
		searchClient.shutdownNow();
	}

	public int getMaxConcurrentRequests(boolean isSearch, String url) {
		try {
			URI uri = new URI(url);
			return isSearch
					? searchClient.maxConcurrentRequestsCallback.getMaxConcurrentRequest(uri)
					: scanClient.maxConcurrentRequestsCallback.getMaxConcurrentRequest(uri);
		} catch (URISyntaxException ex) {
			return -1;
		}
	}
}
