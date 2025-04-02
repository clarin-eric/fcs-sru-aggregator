package eu.clarin.sru.fcs.aggregator.app.configuration;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Range;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import eu.clarin.sru.fcs.aggregator.scan.EndpointConfig;
import io.dropwizard.core.Configuration;

public class AggregatorConfiguration extends Configuration {

    public static class Params {

        @JsonProperty
        String CENTER_REGISTRY_URL;

        @Valid
        @JsonProperty
        List<EndpointConfig> additionalCQLEndpoints;

        @Valid
        @JsonProperty
        List<EndpointConfig> additionalFCSEndpoints;

        @JsonProperty
        List<URI> slowEndpoints;

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
        int SEARCH_MAX_CONCURRENT_REQUESTS_PER_SLOW_ENDPOINT;

        @JsonProperty
        @Range
        int ENDPOINTS_SCAN_TIMEOUT_MS;

        @JsonProperty
        @Range
        int ENDPOINTS_SEARCH_TIMEOUT_MS;

        @JsonProperty
        @Range
        long EXECUTOR_SHUTDOWN_TIMEOUT_MS;

        // ------------------------------------------------------------------

        @NotEmpty
        @JsonProperty
        String AGGREGATOR_FILE_PATH;

        @NotEmpty
        @JsonProperty
        String AGGREGATOR_FILE_PATH_BACKUP;

        @JsonProperty
        boolean prettyPrintJSON;

        // ------------------------------------------------------------------

        @JsonProperty
        String SERVER_URL;

        @JsonProperty
        String VALIDATOR_URL;

        @JsonProperty
        boolean searchResultLinkEnabled;

        @JsonProperty
        boolean openapiEnabled;

        @Valid
        @NotNull
        @JsonProperty
        WeblichtConfig weblichtConfig;

        @Valid
        @NotNull
        @JsonProperty
        PiwikConfig piwikConfig;

        @Valid
        @NotNull
        @JsonProperty
        AAIConfig aaiConfig;

        // ------------------------------------------------------------------

        @JsonIgnore
        public TimeUnit getScanTaskTimeUnit() {
            return TimeUnit.valueOf(SCAN_TASK_TIME_UNIT);
        }

        public String getCENTER_REGISTRY_URL() {
            return CENTER_REGISTRY_URL;
        }

        public int getSCAN_MAX_DEPTH() {
            return SCAN_MAX_DEPTH;
        }

        public long getSCAN_TASK_INITIAL_DELAY() {
            return SCAN_TASK_INITIAL_DELAY;
        }

        public int getSCAN_TASK_INTERVAL() {
            return SCAN_TASK_INTERVAL;
        }

        public String getSCAN_TASK_TIME_UNIT() {
            return SCAN_TASK_TIME_UNIT;
        }

        public long getEXECUTOR_SHUTDOWN_TIMEOUT_MS() {
            return EXECUTOR_SHUTDOWN_TIMEOUT_MS;
        }

        public String getAGGREGATOR_FILE_PATH() {
            return AGGREGATOR_FILE_PATH;
        }

        public String getAGGREGATOR_FILE_PATH_BACKUP() {
            return AGGREGATOR_FILE_PATH_BACKUP;
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
        public int getSEARCH_MAX_CONCURRENT_REQUESTS_PER_SLOW_ENDPOINT() {
            return SEARCH_MAX_CONCURRENT_REQUESTS_PER_SLOW_ENDPOINT;
        }

        @JsonIgnore
        public List<EndpointConfig> getAdditionalCQLEndpoints() {
            return additionalCQLEndpoints;
        }

        @JsonIgnore
        public List<EndpointConfig> getAdditionalFCSEndpoints() {
            return additionalFCSEndpoints;
        }

        @JsonIgnore
        public List<URI> getSlowEndpoints() {
            return slowEndpoints;
        }

        @JsonIgnore
        public boolean getPrettyPrintJSON() {
            return prettyPrintJSON;
        }

        @JsonIgnore
        public boolean getSearchResultLinkEnabled() {
            return searchResultLinkEnabled;
        }

        @JsonIgnore
        public boolean isOpenAPIEnabled() {
            return openapiEnabled;
        }

        @JsonIgnore
        public String getSERVER_URL() {
            return SERVER_URL;
        }

        @JsonIgnore
        public String getVALIDATOR_URL() {
            return VALIDATOR_URL;
        }

        @JsonIgnore
        public WeblichtConfig getWeblichtConfig() {
            return weblichtConfig;
        }

        @JsonIgnore
        public PiwikConfig getPiwikConfig() {
            return piwikConfig;
        }

        @JsonIgnore
        public AAIConfig getAAIConfig() {
            return aaiConfig;
        }
    }

    @Valid
    public Params aggregatorParams = new Params();

    @JsonIgnore
    public Params getAggregatorParams() {
        return aggregatorParams;
    }
}
