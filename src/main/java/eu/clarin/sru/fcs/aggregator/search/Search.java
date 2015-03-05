package eu.clarin.sru.fcs.aggregator.search;

import eu.clarin.sru.client.SRUVersion;
import java.util.List;
import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUSearchRetrieveRequest;
import eu.clarin.sru.client.SRUSearchRetrieveResponse;
import eu.clarin.sru.client.fcs.ClarinFCSRecordData;
import eu.clarin.sru.fcs.aggregator.client.ThrottledClient;
import eu.clarin.sru.fcs.aggregator.scan.Corpus;
import eu.clarin.sru.fcs.aggregator.scan.Diagnostic;
import eu.clarin.sru.fcs.aggregator.scan.FCSProtocolVersion;
import eu.clarin.sru.fcs.aggregator.scan.Statistics;
import eu.clarin.sru.fcs.aggregator.util.SRUCQL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.LoggerFactory;

/**
 * Class representing a search operation
 *
 * @author Yana Panchenko
 * @author edima
 */
public class Search {

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(Search.class);

	private static final String SEARCH_RESULTS_ENCODING = "UTF-8";

	private static final AtomicLong counter = new AtomicLong(Math.abs(new Random().nextInt()));

	private final Long id;
	private final String query;
	private final long createdAt = System.currentTimeMillis();
	private final String searchLanguage;
	private final List<Request> requests = Collections.synchronizedList(new ArrayList<Request>());
	private final List<Result> results = Collections.synchronizedList(new ArrayList<Result>());
	private final Statistics statistics;

	public Search(ThrottledClient searchClient, SRUVersion version,
			Statistics statistics, List<Corpus> corpora, String searchString,
			String searchLanguage, int startRecord, int maxRecords
	) {
		this.id = counter.getAndIncrement();
		this.query = searchString;
		this.searchLanguage = searchLanguage;
		this.statistics = statistics;
		for (Corpus corpus : corpora) {
			executeSearch(searchClient, version, corpus, searchString, startRecord, maxRecords);
		}
	}

	private Request executeSearch(ThrottledClient searchClient, SRUVersion version,
			final Corpus corpus, String searchString,
			int startRecord, int maxRecords) {
		final Request request = new Request(corpus, searchString, startRecord, startRecord + maxRecords - 1);
		log.info("Executing search in '{}' query='{}' maxRecords='{}'", corpus, searchString, maxRecords);

		SRUSearchRetrieveRequest searchRequest = new SRUSearchRetrieveRequest(corpus.getEndpoint().getUrl());
		searchRequest.setVersion(version);
		searchRequest.setMaximumRecords(maxRecords);
		boolean legacy = corpus.getEndpoint().getProtocol().equals(FCSProtocolVersion.LEGACY);
		searchRequest.setRecordSchema(legacy
				? ClarinFCSRecordData.LEGACY_RECORD_SCHEMA
				: ClarinFCSRecordData.RECORD_SCHEMA);
		searchRequest.setQuery(searchString);
		searchRequest.setStartRecord(startRecord);
		if (corpus.getHandle() != null) {
			searchRequest.setExtraRequestData(legacy
					? SRUCQL.SEARCH_CORPUS_HANDLE_LEGACY_PARAMETER
					: SRUCQL.SEARCH_CORPUS_HANDLE_PARAMETER,
					corpus.getHandle());
		}
		requests.add(request);

		try {
			searchClient.searchRetrieve(searchRequest, new ThrottledClient.SearchCallback() {
				@Override
				public void onSuccess(SRUSearchRetrieveResponse response, ThrottledClient.Stats stats) {
					try {
						statistics.addEndpointDatapoint(corpus.getInstitution(), corpus.getEndpoint(), stats.getQueueTime(), stats.getExecutionTime());
						Result result = new Result(request, response, null);
						results.add(result);
						requests.remove(request);
						List<Diagnostic> diagnostics = result.getDiagnostics();
						if (diagnostics != null && !diagnostics.isEmpty()) {
							log.error("diagnostic for url: " + response.getRequest().getRequestedURI().toString());
							for (Diagnostic diagnostic : diagnostics) {
								statistics.addEndpointDiagnostic(corpus.getInstitution(), corpus.getEndpoint(),
										diagnostic, response.getRequest().getRequestedURI().toString());
							}
						}
					} catch (Throwable xc) {
						log.error("search.onSuccess exception:", xc);
					}
				}

				@Override
				public void onError(SRUSearchRetrieveRequest srureq, SRUClientException xc, ThrottledClient.Stats stats) {
					try {
						statistics.addEndpointDatapoint(corpus.getInstitution(), corpus.getEndpoint(), stats.getQueueTime(), stats.getExecutionTime());
						statistics.addErrorDatapoint(corpus.getInstitution(), corpus.getEndpoint(), xc, srureq.getRequestedURI().toString());
						results.add(new Result(request, null, xc));
						requests.remove(request);
						log.error("search.onError: ", xc);
					} catch (Throwable xxc) {
						log.error("search.onError exception:", xxc);
					}
				}
			});
		} catch (Throwable xc) {
			log.error("SearchRetrieve error for " + corpus.getEndpoint().getUrl(), xc);
		}
		return request;
	}

	public Long getId() {
		return id;
	}

	public List<Request> getRequests() {
		List<Request> copy = new ArrayList<>();
		synchronized (requests) {
			copy.addAll(requests);
		}
		return copy;
	}

	public List<Result> getResults(String corpusId) {
		List<Result> copy = new ArrayList<>();
		synchronized (results) {
			if (corpusId == null || corpusId.isEmpty()) {
				copy.addAll(results);
			} else {
				for (Result r : results) {
					if (corpusId.equals(r.getCorpus().getId())) {
						copy.add(r);
					}
				}
			}
		}
		return copy;
	}

	public Statistics getStatistics() {
		return statistics;
	}

	public void shutdown() {
		// nothing to do 
	}

	public long getCreatedAt() {
		return createdAt;
	}

	public String getQuery() {
		return query;
	}

	public String getSearchLanguage() {
		return searchLanguage;
	}

}
