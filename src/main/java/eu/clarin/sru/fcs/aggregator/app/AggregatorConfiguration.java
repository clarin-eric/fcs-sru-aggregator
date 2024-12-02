package eu.clarin.sru.fcs.aggregator.app;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.dropwizard.core.Configuration;

import java.io.IOException;
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

        public static class EndpointConfig {
            @NotNull
            @JsonProperty
            URL url;

            @JsonProperty
            String name;

            @JsonProperty
            String website;

            protected EndpointConfig(String url) throws MalformedURLException {
                this.url = new URL(url);
            }

            protected EndpointConfig() {
            }

            @JsonIgnore
            public URL getUrl() {
                return url;
            }

            @JsonIgnore
            public String getName() {
                return name;
            }

            @JsonIgnore
            public String getWebsite() {
                return website;
            }
        }

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

        public static class AAIConfig {
            @JsonProperty
            boolean enabled;

            @JsonIgnore
            public boolean isAAIEnabled() {
                return enabled;
            }

            @JsonProperty
            String shibWebappHost;

            @JsonIgnore
            public String getShibWebappHost() {
                return shibWebappHost;
            }

            @JsonProperty
            String shibLogin;

            @JsonIgnore
            public String getShibLogin() {
                return shibLogin;
            }

            @JsonProperty
            String shibLogout;

            @JsonIgnore
            public String getShibLogout() {
                return shibLogout;
            }

            public static class KeyConfig {
                @JsonProperty
                @JsonDeserialize(using = PEMKeyStringDeserializer.class)
                String publicKey;

                @JsonIgnore
                public String getPublicKey() {
                    return publicKey;
                }

                // TODO: better masking with
                // https://stackoverflow.com/questions/56070451/mask-json-fields-using-jackson
                @JsonProperty(access = Access.WRITE_ONLY)
                @JsonDeserialize(using = PEMKeyStringDeserializer.class)
                String privateKey;

                @JsonIgnore
                public String getPrivateKey() {
                    return privateKey;
                }

                // TODO: maybe key path properties?
                // TODO: validation?

                /**
                 * Helper to correctly deserialize PEM keys when e.g., supplied via environment
                 * variable substitution which doesn't correctly preserve the line breaks.
                 */
                public static class PEMKeyStringDeserializer extends JsonDeserializer<String> {
                    private final static String MARKER_DELIMS = "-----";
                    private final static int MARKER_DELIMS_LEN = MARKER_DELIMS.length();

                    @Override
                    public String deserialize(JsonParser p, DeserializationContext ctxt)
                            throws IOException, JacksonException {
                        String contents = p.getText().strip();
                        if (!contents.startsWith(MARKER_DELIMS) || !contents.endsWith(MARKER_DELIMS)) {
                            throw new JsonParseException(p, "Expected PEM key block with '-----' delimiters!");
                        }
                        if (contents.indexOf('\n') != -1) {
                            // has line breaks, we do nothing
                            return contents;
                        }
                        // no line breaks
                        if (contents.indexOf("\\n") != -1) {
                            contents = contents.replace("\\n", "\n");
                            return contents;
                        }
                        // probably line breaks converted to single spaces
                        int posStartOfStartBlock = contents.indexOf(MARKER_DELIMS + "BEGIN ");
                        if (posStartOfStartBlock == -1) {
                            throw new JsonParseException(p,
                                    "Unexpected PEM structure, expected '-----BEGIN ' start of BEGIN block indicator!");
                        }
                        int posEndOfStartBlock = contents.indexOf(MARKER_DELIMS,
                                posStartOfStartBlock + MARKER_DELIMS_LEN + 6 + 1);
                        if (posEndOfStartBlock == -1) {
                            throw new JsonParseException(p,
                                    "Unexpected PEM structure, expected '----- ' end of BEGIN block indicator!");
                        }

                        int posStartOfEndBlock = contents.indexOf(MARKER_DELIMS + "END ",
                                posEndOfStartBlock + MARKER_DELIMS_LEN + 1);
                        if (posStartOfEndBlock == -1) {
                            throw new JsonParseException(p,
                                    "Unexpected PEM structure, expected '-----END ' start of END block indicator!");
                        }
                        int posEndOfEndBlock = contents.indexOf(MARKER_DELIMS,
                                posStartOfEndBlock + MARKER_DELIMS_LEN + 4 + 1);
                        if (posEndOfEndBlock == -1) {
                            throw new JsonParseException(p,
                                    "Unexpected PEM structure, expected '-----' end of END block indicator!");
                        }

                        int posStartContents = posEndOfStartBlock + MARKER_DELIMS_LEN;
                        int posEndContents = posStartOfEndBlock;

                        String header = contents.substring(0, posEndOfStartBlock + MARKER_DELIMS_LEN);
                        String footer = contents.substring(posStartOfEndBlock);
                        String blockContents = contents.substring(posStartContents, posEndContents).strip().replace(' ',
                                '\n');
                        return header + "\n" + blockContents + "\n" + footer;
                    }
                }
            }

            @Valid
            @NotNull
            @JsonProperty
            KeyConfig keys;
        }

        @Valid
        @NotNull
        @JsonProperty
        AAIConfig aaiConfig;
    }

    @Valid
    public Params aggregatorParams = new Params();
}
