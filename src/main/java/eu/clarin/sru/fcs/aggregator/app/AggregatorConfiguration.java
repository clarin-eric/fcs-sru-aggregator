package eu.clarin.sru.fcs.aggregator.app;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.Range;

public class AggregatorConfiguration extends Configuration {

	public static class Params {

		@JsonProperty
		String CENTER_REGISTRY_URL;

		@JsonProperty
		List<URL> additionalCQLEndpoints;

		@NotEmpty
		@JsonProperty
		String AGGREGATOR_FILE_PATH;

		@NotEmpty
		@JsonProperty
		String AGGREGATOR_FILE_PATH_BACKUP;

		@JsonProperty
		@Range
		int SCAN_MAX_DEPTH;

		@JsonProperty
		@Range
		long SCAN_TASK_INITIAL_DELAY;

		@Range
		@JsonProperty
		int SCAN_TASK_INTERVAL;

		@NotEmpty
		@JsonProperty
		String SCAN_TASK_TIME_UNIT;

		@JsonProperty
		@Range
		int SCAN_MAX_CONCURRENT_REQUESTS_PER_ENDPOINT;

		@JsonProperty
		@Range
		int SEARCH_MAX_CONCURRENT_REQUESTS_PER_ENDPOINT;

		@JsonProperty
		@Range
		int ENDPOINTS_SCAN_TIMEOUT_MS;

		@JsonProperty
		@Range
		int ENDPOINTS_SEARCH_TIMEOUT_MS;

		@JsonProperty
		@Range
		long EXECUTOR_SHUTDOWN_TIMEOUT_MS;

		public static class WeblichtConfig {
			@JsonProperty
			String url;

			@JsonProperty
			List<String> acceptedTcfLanguages;

			@JsonIgnore
			public String getUrl() {
				return url;
			}

			@JsonIgnore
			public List<String> getAcceptedTcfLanguages() {
				return acceptedTcfLanguages;
			}
		}

		@NotEmpty
		@JsonProperty
		WeblichtConfig weblichtConfig;

		@JsonIgnore
		public TimeUnit getScanTaskTimeUnit() {
			return TimeUnit.valueOf(SCAN_TASK_TIME_UNIT);
		}

		@JsonIgnore
		public int getENDPOINTS_SCAN_TIMEOUT_MS() {
			return ENDPOINTS_SCAN_TIMEOUT_MS;
		}

		@JsonIgnore
		public int getENDPOINTS_SEARCH_TIMEOUT_MS() {
			return ENDPOINTS_SEARCH_TIMEOUT_MS;
		}

		@JsonIgnore
		public int getSCAN_MAX_CONCURRENT_REQUESTS_PER_ENDPOINT() {
			return SCAN_MAX_CONCURRENT_REQUESTS_PER_ENDPOINT;
		}

		@JsonIgnore
		public int getSEARCH_MAX_CONCURRENT_REQUESTS_PER_ENDPOINT() {
			return SEARCH_MAX_CONCURRENT_REQUESTS_PER_ENDPOINT;
		}

		@JsonIgnore
		public WeblichtConfig getWeblichtConfig() {
			return weblichtConfig;
		}
	}
	public Params aggregatorParams = new Params();
}
