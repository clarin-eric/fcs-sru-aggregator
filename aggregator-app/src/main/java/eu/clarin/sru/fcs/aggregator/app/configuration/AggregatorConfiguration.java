package eu.clarin.sru.fcs.aggregator.app.configuration;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Range;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import eu.clarin.sru.fcs.aggregator.core.AggregatorParams;

public class AggregatorConfiguration implements AggregatorParams {

    @Valid
    @NotNull
    @JsonProperty
    private ScanConfiguration scan = new ScanConfiguration();

    @Valid
    @NotNull
    @JsonProperty
    private SearchConfiguration search = new SearchConfiguration();

    @Valid
    @JsonProperty
    private WeblichtConfiguration weblicht;

    @Valid
    @NotNull
    @JsonProperty
    private MatomoConfiguration matomo = new MatomoConfiguration();

    @Valid
    @NotNull
    @JsonProperty
    private AuthConfiguration auth = new AuthConfiguration();

    @Valid
    @JsonProperty
    private List<EndpointOverrideConfiguration> endpointOverrides;

    @Range
    @JsonProperty
    private long executorShutdownTimeoutMs = 1_000;

    @JsonProperty
    private boolean prettyPrintJSON = false;

    @JsonProperty
    private boolean searchResultLinkEnabled = false;

    @JsonProperty
    private boolean openapiEnabled = false;

    @NotNull
    @JsonProperty
    private String serverUrl;

    @JsonProperty
    private String validatorUrl;

    @JsonProperty
    private boolean echoConfig = false;

    // ----------------------------------------------------------------------

    @JsonProperty("scan")
    public ScanConfiguration getScanConfiguration() {
        return scan;
    }

    @JsonProperty("search")
    public SearchConfiguration getSearchConfiguration() {
        return search;
    }

    @JsonProperty("weblicht")
    public WeblichtConfiguration getWeblichtConfiguration() {
        return weblicht;
    }

    @JsonProperty("matomo")
    public MatomoConfiguration getMatomoConfiguration() {
        return matomo;
    }

    @JsonProperty("auth")
    public AuthConfiguration getAuthConfiguration() {
        return auth;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @JsonProperty("endpointOverrides")
    @Override
    public List getEndpointOverrides() {
        if (endpointOverrides == null) {
            return Collections.emptyList();
        }
        return endpointOverrides;
    }

    @JsonProperty("executorShutdownTimeoutMs")
    public long getExecutorShutdownTimeoutMs() {
        return executorShutdownTimeoutMs;
    }

    @JsonProperty("prettyPrintJSON")
    public boolean isPrettyPrintJSON() {
        return prettyPrintJSON;
    }

    @JsonProperty("searchResultLinkEnabled")
    public boolean isSearchResultLinkEnabled() {
        return searchResultLinkEnabled;
    }

    @JsonProperty("openapiEnabled")
    public boolean isOpenapiEnabled() {
        return openapiEnabled;
    }

    @JsonProperty("serverUrl")
    public String getServerUrl() {
        return serverUrl;
    }

    @JsonProperty("validatorUrl")
    public String getValidatorUrl() {
        return validatorUrl;
    }

    @JsonProperty("echoConfig")
    public boolean isEchoConfig() {
        return echoConfig;
    }

    // ----------------------------------------------------------------------

    @JsonIgnore
    @Override
    public long getExecutorShutdownTimeout() {
        return getExecutorShutdownTimeoutMs();
    }

    @JsonIgnore
    @Override
    public int getEndpointScanTimeout() {
        return getScanConfiguration().getRequestTimeoutMs();
    }

    @JsonIgnore
    @Override
    public int getEndpointSearchTimeout() {
        return getSearchConfiguration().getRequestTimeoutMs();
    }

    @JsonIgnore
    @Override
    public int getMaxConcurrentScanRequestsPerEndpoint() {
        return getScanConfiguration().getMaxConcurrentRequests();
    }

    @JsonIgnore
    @Override
    public int getMaxConcurrentSearchRequestsPerEndpoint() {
        return getSearchConfiguration().getMaxConcurrentRequests();
    }

    @JsonIgnore
    @Override
    public String getCenterRegistryUrl() {
        return getScanConfiguration().getCentreRegistryUrl();
    }

    @JsonIgnore
    @Override
    public int getScanMaxDepth() {
        return getScanConfiguration().getMaxScanDepth();
    }

    @JsonIgnore
    @Override
    public long getScanTaskInitialDelay() {
        return getScanConfiguration().getScanTaskInitialDelay();
    }

    @JsonIgnore
    @Override
    public long getScanTaskInterval() {
        return getScanConfiguration().getScanTaskInterval();
    }

    @JsonIgnore
    @Override
    public TimeUnit getScanTaskTimeUnit() {
        return getScanConfiguration().getScanTaskIntervalTimeUnit();
    }

    @JsonIgnore
    @Override
    public boolean enableScanCrawlTask() {
        return true;
    }

    @JsonIgnore
    @Override
    public int getSearchesSizeThreshold() {
        return getSearchConfiguration().getMaxAmountSearchCache();
    }

    @JsonIgnore
    @Override
    public long getSearchesAgeThreshold() {
        return getSearchConfiguration().getMaxAgeSearchCacheMs();
    }

    @JsonIgnore
    @Override
    public boolean isAAIEnabled() {
        return getAuthConfiguration().isEnabled();
    }

    @JsonIgnore
    @Override
    public String getPublicKey() {
        final AuthConfiguration authConfig = getAuthConfiguration();
        if (authConfig.getKeys() == null) {
            return null;
        }
        return authConfig.getKeys().getPublicKey();
    }

    @JsonIgnore
    @Override
    public String getPrivateKey() {
        final AuthConfiguration authConfig = getAuthConfiguration();
        if (authConfig.getKeys() == null) {
            return null;
        }
        return authConfig.getKeys().getPrivateKey();
    }

    @JsonIgnore
    @Override
    public String getPublicKeyFile() {
        final AuthConfiguration authConfig = getAuthConfiguration();
        if (authConfig.getKeys() == null) {
            return null;
        }
        return authConfig.getKeys().getPublicKeyFile();
    }

    @JsonIgnore
    @Override
    public String getPrivateKeyFile() {
        final AuthConfiguration authConfig = getAuthConfiguration();
        if (authConfig.getKeys() == null) {
            return null;
        }
        return authConfig.getKeys().getPrivateKeyFile();
    }

}
