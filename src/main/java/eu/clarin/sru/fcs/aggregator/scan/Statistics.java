package eu.clarin.sru.fcs.aggregator.scan;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author edima
 * @author ljo
 *
 * Stores statistics information about scans or searches. The info is then sent
 * to the JS client and displayed in the /Aggregator/stats page.
 */
public class Statistics {

	public static class EndpointStats {

		private final Object lock = new Object();

		@JsonProperty
		FCSProtocolVersion version = FCSProtocolVersion.LEGACY;

		@JsonProperty
		List<String> rootCollections = new ArrayList<String>();

		List<Long> queueTimes = Collections.synchronizedList(new ArrayList<Long>());
		List<Long> executionTimes = Collections.synchronizedList(new ArrayList<Long>());

		@JsonProperty
		int maxConcurrentRequests;

		public static class DiagPair {

			public DiagPair(Diagnostic diagnostic, String context, int counter) {
				this.diagnostic = diagnostic;
				this.context = context;
				this.counter = counter;
			}

			@JsonProperty
			public Diagnostic diagnostic;
			@JsonProperty
			String context;
			@JsonProperty
			public int counter;
		}

		@JsonProperty
		Map<String, DiagPair> diagnostics = Collections.synchronizedMap(new HashMap<String, DiagPair>());

		public static class ExcPair {

			public ExcPair(JsonException exception, String context, int counter) {
				this.exception = exception;
				this.context = context;
				this.counter = counter;
			}

			@JsonProperty
			public JsonException exception;
			@JsonProperty
			String context;
			@JsonProperty
			public int counter;
		}

		@JsonProperty
		Map<String, ExcPair> errors = Collections.synchronizedMap(new HashMap<String, ExcPair>());

		double avg(List<Long> q) {
			double sum = 0;
			for (long l : q) {
				sum += l;
			}
			return sum / q.size();
		}

		double max(List<Long> q) {
			double max = 0;
			for (long l : q) {
				max = max > l ? max : l;
			}
			return max;
		}

		@JsonProperty
		public double getAvgQueueTime() {
			return avg(queueTimes) / 1000.0;
		}

		@JsonProperty
		public double getMaxQueueTime() {
			return max(queueTimes) / 1000.0;
		}

		@JsonProperty
		public double getAvgExecutionTime() {
			return avg(executionTimes) / 1000.0;
		}

		@JsonProperty
		public double getMaxExecutionTime() {
			return max(executionTimes) / 1000.0;
		}

		@JsonProperty
		public int getNumberOfRequests() {
			return executionTimes.size();
		}
	};

	private final Object lock = new Object();

	Date date = new Date();

	public Date getDate() {
		return date;
	}

	// institution to endpoint to statistics_per_endpoint map
	Map<String, Map<String, EndpointStats>> institutions
			= Collections.synchronizedMap(new HashMap<String, Map<String, EndpointStats>>());

	public Map<String, Map<String, EndpointStats>> getInstitutions() {
		return institutions;
	}

	public void initEndpoint(Institution institution, Endpoint endpoint, int maxConcurrentRequests) {
		EndpointStats stats = getEndpointStats(institution, endpoint);
		synchronized (stats.lock) {
			stats.maxConcurrentRequests = maxConcurrentRequests;
		}
	}

	public void addEndpointDatapoint(Institution institution, Endpoint endpoint, long enqueuedTime, long executionTime) {
		EndpointStats stats = getEndpointStats(institution, endpoint);
		synchronized (stats.lock) {
			stats.queueTimes.add(enqueuedTime);
			stats.executionTimes.add(executionTime);
		}
	}

	public void addEndpointDiagnostic(Institution institution, Endpoint endpoint, Diagnostic diag, String context) {
		EndpointStats stats = getEndpointStats(institution, endpoint);
		synchronized (stats.lock) {
			if (!stats.diagnostics.containsKey(diag.uri)) {
				stats.diagnostics.put(diag.uri, new EndpointStats.DiagPair(diag, context, 1));
			} else {
				stats.diagnostics.get(diag.uri).counter++;
			}
		}
	}

	public void addErrorDatapoint(Institution institution, Endpoint endpoint, Exception error, String context) {
		EndpointStats stats = getEndpointStats(institution, endpoint);
		JsonException jxc = new JsonException(error);
		synchronized (stats.lock) {
			if (!stats.errors.containsKey(jxc.message)) {
				stats.errors.put(jxc.message, new EndpointStats.ExcPair(jxc, context, 1));
			} else {
				stats.errors.get(jxc.message).counter++;
			}
		}
	}

	public void upgradeProtocolVersion(Institution institution, Endpoint endpoint) {
		EndpointStats stats = getEndpointStats(institution, endpoint);
		synchronized (stats.lock) {
		    stats.version = endpoint.getProtocol() == FCSProtocolVersion.VERSION_2 ? FCSProtocolVersion.VERSION_2 : FCSProtocolVersion.VERSION_1;
		}
	}

	public void addEndpointCollection(Institution institution, Endpoint endpoint, String collectionName) {
		EndpointStats stats = getEndpointStats(institution, endpoint);
		synchronized (stats.lock) {
			stats.rootCollections.add(collectionName);
		}
	}

	public void addEndpointCollections(Institution institution, Endpoint endpoint, List<String> collections) {
		EndpointStats stats = getEndpointStats(institution, endpoint);
		synchronized (stats.lock) {
			stats.rootCollections.addAll(collections);
		}
	}

	private EndpointStats getEndpointStats(Institution institution, Endpoint endpoint) {
		EndpointStats stats;
		synchronized (lock) {
			if (!institutions.containsKey(institution.getName())) {
				institutions.put(institution.getName(),
						Collections.synchronizedMap(new HashMap<String, EndpointStats>()));
			}
			Map<String, EndpointStats> esmap = institutions.get(institution.getName());
			if (!esmap.containsKey(endpoint.getUrl())) {
				EndpointStats es = new EndpointStats();
				es.version = endpoint.getProtocol();
				esmap.put(endpoint.getUrl(), es);
			}
			stats = esmap.get(endpoint.getUrl());
		}
		return stats;
	}

}
