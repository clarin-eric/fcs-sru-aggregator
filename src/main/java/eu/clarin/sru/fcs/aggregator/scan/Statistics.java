package eu.clarin.sru.fcs.aggregator.scan;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.clarin.sru.client.SRUClientException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author edima
 */
public class Statistics {

	public static class EndpointStats {

		@JsonProperty
		FCSProtocolVersion version = FCSProtocolVersion.LEGACY;

		List<Long> queueTimes = Collections.synchronizedList(new ArrayList<Long>());
		List<Long> executionTimes = Collections.synchronizedList(new ArrayList<Long>());

		public static class DiagPair {

			public DiagPair(Diagnostic diagnostic, int counter) {
				this.diagnostic = diagnostic;
				this.counter = counter;
			}

			public Diagnostic diagnostic;
			public int counter;
		}

		@JsonProperty
		Map<String, DiagPair> diagnostics = Collections.synchronizedMap(new HashMap<String, DiagPair>());
		@JsonProperty
		Map<String, Integer> errors = Collections.synchronizedMap(new HashMap<String, Integer>());

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

	// institution to endpoint to statistics_per_endpoint map
	Map<String, Map<String, EndpointStats>> institutions
			= Collections.synchronizedMap(new HashMap<String, Map<String, EndpointStats>>());

	public Map<String, Map<String, EndpointStats>> getInstitutions() {
		return institutions;
	}

	public void addEndpointDatapoint(Institution institution, Endpoint endpoint, long enqueuedTime, long executionTime) {
		EndpointStats stats = getEndpointStats(institution, endpoint);
		stats.queueTimes.add(enqueuedTime);
		stats.executionTimes.add(executionTime);
	}

	public void addEndpointDiagnostic(Institution institution, Endpoint endpoint, Diagnostic diag) {
		EndpointStats stats = getEndpointStats(institution, endpoint);
		if (!stats.diagnostics.containsKey(diag.getDgnUri())) {
			stats.diagnostics.put(diag.getDgnUri(), new EndpointStats.DiagPair(diag, 1));
		} else {
			stats.diagnostics.get(diag.getDgnUri()).counter++;
		}
	}

	public void addErrorDatapoint(Institution institution, Endpoint endpoint, SRUClientException error) {
		EndpointStats stats = getEndpointStats(institution, endpoint);
		int number = 0;
		if (stats.errors.containsKey(error.getMessage())) {
			number = stats.errors.get(error.getMessage());
		}
		stats.errors.put(error.getMessage(), number + 1);
	}

	public void upgradeProtocolVersion(Institution institution, Endpoint endpoint) {
		EndpointStats stats = getEndpointStats(institution, endpoint);
		stats.version = FCSProtocolVersion.VERSION_1;
	}

	private EndpointStats getEndpointStats(Institution institution, Endpoint endpoint) {
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
		return esmap.get(endpoint.getUrl());
	}
}
