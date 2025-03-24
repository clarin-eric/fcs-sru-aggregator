package eu.clarin.sru.fcs.aggregator.app;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import eu.clarin.sru.fcs.aggregator.scan.EndpointConfig;
import io.dropwizard.core.Configuration;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Range;

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

        public static class WeblichtConfig {
            @JsonProperty
            String url;

            @JsonProperty
            String exportServerUrl;

            @JsonProperty
            List<String> acceptedTcfLanguages;

            @JsonIgnore
            public String getUrl() {
                return url;
            }

            @JsonIgnore
            public String getExportServerUrl() {
                return exportServerUrl;
            }

            @JsonIgnore
            public List<String> getAcceptedTcfLanguages() {
                return acceptedTcfLanguages;
            }
        }

        @Valid
        @NotNull
        @JsonProperty
        WeblichtConfig weblichtConfig;

        @JsonIgnore
        public WeblichtConfig getWeblichtConfig() {
            return weblichtConfig;
        }

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
        public int getSEARCH_MAX_CONCURRENT_REQUESTS_PER_SLOW_ENDPOINT() {
            return SEARCH_MAX_CONCURRENT_REQUESTS_PER_SLOW_ENDPOINT;
        }

        @JsonProperty
        boolean prettyPrintJSON;

        @JsonIgnore
        public boolean getPrettyPrintJSON() {
            return prettyPrintJSON;
        }

        public static class PiwikConfig {
            @JsonProperty
            boolean enabled;

            @JsonProperty
            String url;

            @JsonProperty
            int siteID;

            @JsonProperty
            List<String> setDomains;

            @JsonIgnore
            public boolean isEnabled() {
                return enabled;
            }

            @JsonIgnore
            public String getUrl() {
                return url;
            }

            @JsonIgnore
            public int getSiteID() {
                return siteID;
            }

            @JsonIgnore
            public List<String> getSetDomains() {
                return setDomains;
            }
        }

        @Valid
        @NotNull
        @JsonProperty
        PiwikConfig piwikConfig;

        @JsonIgnore
        public PiwikConfig getPiwikConfig() {
            return piwikConfig;
        }

        @JsonProperty
        boolean searchResultLinkEnabled;

        @JsonIgnore
        public boolean getSearchResultLinkEnabled() {
            return searchResultLinkEnabled;
        }

        @JsonProperty
        boolean openapiEnabled;

        @JsonIgnore
        public boolean isOpenAPIEnabled() {
            return openapiEnabled;
        }

        @JsonProperty
        String SERVER_URL;

        @JsonIgnore
        public String getSERVER_URL() {
            return SERVER_URL;
        }

        @JsonProperty
        String VALIDATOR_URL;

        @JsonIgnore
        public String getVALIDATOR_URL() {
            return VALIDATOR_URL;
        }
    }

    @Valid
    public Params aggregatorParams = new Params();
}
