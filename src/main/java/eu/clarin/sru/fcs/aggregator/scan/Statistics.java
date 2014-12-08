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

		List<Long> queueTimes = Collections.synchronizedList(new ArrayList<Long>());
		List<Long> executionTimes = Collections.synchronizedList(new ArrayList<Long>());
		List<Throwable> throwables = Collections.synchronizedList(new ArrayList<Throwable>());

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

		public double getAvgQueueTime() {
			return avg(queueTimes);
		}

		public double getMaxQueueTime() {
			return max(queueTimes);
		}

		public double getAvgExecutionTime() {
			return avg(executionTimes);
		}

		public double getMaxExecutionTime() {
			return max(executionTimes);
		}
	};

	// institution to endpoint to statistics_per_endpoint map
	@JsonProperty
	Map<String, Map<String, EndpointStats>> institutions
			= Collections.synchronizedMap(new HashMap<String, Map<String, EndpointStats>>());

	public void addEndpointDatapoint(Institution institution, String endpoint, long enqueuedTime, long executionTime) {
		EndpointStats stats = getEndpointStats(institution, endpoint);
		stats.queueTimes.add(enqueuedTime);
		stats.executionTimes.add(executionTime);
	}

	public void addErrorDatapoint(Institution institution, String endpoint, SRUClientException error) {
		EndpointStats stats = getEndpointStats(institution, endpoint);
		stats.throwables.add(error);
	}

	private EndpointStats getEndpointStats(Institution institution, String endpoint) {
		if (!institutions.containsKey(institution.getName())) {
			institutions.put(institution.getName(),
					Collections.synchronizedMap(new HashMap<String, EndpointStats>()));
		}
		Map<String, EndpointStats> esmap = institutions.get(institution.getName());
		if (!esmap.containsKey(endpoint)) {
			esmap.put(endpoint, new EndpointStats());
		}
		return esmap.get(endpoint);
	}

}
