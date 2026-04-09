package eu.clarin.sru.fcs.aggregator.core;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;

import org.slf4j.LoggerFactory;

import eu.clarin.sru.client.SRURequestAuthenticator;
import eu.clarin.sru.client.SRUThreadedClient;
import eu.clarin.sru.client.SRUVersion;
import eu.clarin.sru.client.auth.ClarinFCSRequestAuthenticator;
import eu.clarin.sru.client.fcs.ClarinFCSClientBuilder;
import eu.clarin.sru.client.fcs.ClarinFCSEndpointDescriptionParser;
import eu.clarin.sru.fcs.aggregator.client.MaxConcurrentRequestsCallback;
import eu.clarin.sru.fcs.aggregator.client.ThrottledClient;
import eu.clarin.sru.fcs.aggregator.scan.EndpointFilter;
import eu.clarin.sru.fcs.aggregator.scan.EndpointOverrideConfig;
import eu.clarin.sru.fcs.aggregator.scan.Resource;
import eu.clarin.sru.fcs.aggregator.scan.ScanCrawlTask;
import eu.clarin.sru.fcs.aggregator.scan.ScanCrawlTask.ScanCrawlTaskCompletedCallback;
import eu.clarin.sru.fcs.aggregator.scan.Statistics;
import eu.clarin.sru.fcs.aggregator.search.PerformLanguageDetectionCallback;
import eu.clarin.sru.fcs.aggregator.search.Search;

public abstract class AggregatorBase {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(AggregatorBase.class);

    // ----------------------------------------------------------------------
    // creators

    public static SRURequestAuthenticator createSRURequestAuthenticator(FCSAuthenticationParams params) {
        final SRURequestAuthenticator requestAuthStrategy;

        if (params.isAAIEnabled()) {
            final ClarinFCSRequestAuthenticator.AuthenticationInfoProvider authInfoPovider = new ClarinFCSRequestAuthenticator.AuthenticationInfoProvider() {
                @Override
                public String getAudience(String endpointURI, Map<String, String> context) {
                    return endpointURI; // default behaviour, aud is endpoint url
                }

                @Override
                public String getSubject(String endpointURI, Map<String, String> context) {
                    if (context != null) {
                        return context.get(AggregatorConstants.PARAM_AUTHINFO_USERID);
                    }
                    return null;
                }
            };

            ClarinFCSRequestAuthenticator.Builder requestAuthStrategyBuilder = ClarinFCSRequestAuthenticator.Builder
                    .create()
                    .withIssuer(params.getServerUrl())
                    .withAuthenticationInfoProvider(authInfoPovider);

            // TODO: change to Public|PrivateKey interfaces (should be more secure!)
            if (params.getPublicKey() != null && !params.getPublicKey().strip().isBlank()
                    && params.getPrivateKey() != null && !params.getPrivateKey().strip().isBlank()) {
                requestAuthStrategyBuilder = requestAuthStrategyBuilder
                        .withKeyPairContents(params.getPublicKey(), params.getPrivateKey());
            } else if (params.getPublicKeyFile() != null && params.getPrivateKeyFile() != null) {
                requestAuthStrategyBuilder = requestAuthStrategyBuilder
                        .withKeyPair(params.getPublicKeyFile(), params.getPrivateKeyFile());
            }

            requestAuthStrategy = requestAuthStrategyBuilder.build();
        } else {
            requestAuthStrategy = null;
        }

        return requestAuthStrategy;
    }

    public static ThrottledClient createClient(SRUFCSClientParams params, SRURequestAuthenticator requestAuthStrategy) {
        return createClient(params.getEndpointScanTimeout(), params.getEndpointSearchTimeout(),
                params.getMaxConcurrentScanRequestsPerEndpoint(), params.getMaxConcurrentSearchRequestsPerEndpoint(),
                params.getEndpointOverrides(), requestAuthStrategy);
    }

    public static ThrottledClient createClient(int endpointScanTimeout, int endpointSearchTimeout,
            int maxConcurrentScanRequestsPerEndpoint, int maxConcurrentSearchRequestsPerEndpoint,
            List<EndpointOverrideConfig> endpointOverrides, SRURequestAuthenticator requestAuthStrategy) {

        final SRUThreadedClient sruScanClient = new ClarinFCSClientBuilder()
                .setConnectTimeout(endpointScanTimeout)
                .setSocketTimeout(endpointScanTimeout)
                .addDefaultDataViewParsers()
                .registerExtraResponseDataParser(
                        new ClarinFCSEndpointDescriptionParser())
                .enableLegacySupport()
                .setRequestAuthenticator(requestAuthStrategy)
                .buildThreadedClient();

        final SRUThreadedClient sruSearchClient = new ClarinFCSClientBuilder()
                .setConnectTimeout(endpointSearchTimeout)
                .setSocketTimeout(endpointSearchTimeout)
                .addDefaultDataViewParsers()
                .registerExtraResponseDataParser(
                        new ClarinFCSEndpointDescriptionParser())
                .enableLegacySupport()
                .setRequestAuthenticator(requestAuthStrategy)
                .buildThreadedClient();

        final Map<URI, EndpointOverrideConfig> endpointUrl2OverrideLookup = (endpointOverrides != null)
                ? endpointOverrides.stream()
                        .filter(o -> {
                            try {
                                o.getUrl().toURI();
                                return true;
                            } catch (URISyntaxException e) {
                                log.warn("Converting Endpoint URL to URI failed!? '{}'", o.getUrl());
                                return false;
                            }
                        })
                        .collect(Collectors.toMap(o -> {
                            try {
                                return o.getUrl().toURI();
                            } catch (URISyntaxException e) {
                                log.error("Unreachable! Conversion of URL to URI should not have failed here! '{}'",
                                        o.getUrl());
                            }
                            return null;
                        }, o -> o))
                : Collections.emptyMap();

        final MaxConcurrentRequestsCallback maxScanConcurrentRequestsCallback = new MaxConcurrentRequestsCallback() {
            @Override
            public int getMaxConcurrentRequest(URI baseURI) {
                return maxConcurrentScanRequestsPerEndpoint;
            }
        };

        final MaxConcurrentRequestsCallback maxSearchConcurrentRequestsCallback = new MaxConcurrentRequestsCallback() {
            @Override
            public int getMaxConcurrentRequest(URI baseURI) {
                EndpointOverrideConfig override = endpointUrl2OverrideLookup.get(baseURI);
                if (override != null) {
                    final int maxConcurrentRequests = override.getMaxConcurrentSearchRequests();
                    if (maxConcurrentRequests > 0) {
                        return maxConcurrentRequests;
                    }
                }
                return maxConcurrentSearchRequestsPerEndpoint;
            }
        };

        final ThrottledClient sruClient = new ThrottledClient(
                sruScanClient, maxScanConcurrentRequestsCallback,
                sruSearchClient, maxSearchConcurrentRequestsCallback);

        return sruClient;
    }

    // ----------------------------------------------------------------------
    // scan crawl

    public static ScanCrawlTask createScanCrawlTask(ThrottledClient sruClient, Client jerseyClient,
            ScanCrawlTaskParams params, EndpointFilter filter,
            ScanCrawlTaskCompletedCallback scanCrawlTaskCompletedCallback) {
        return createScanCrawlTask(sruClient, jerseyClient,
                params.getCenterRegistryUrl(),
                params.getScanMaxDepth(),
                params.getEndpointOverrides(),
                filter, scanCrawlTaskCompletedCallback);
    }

    public static ScanCrawlTask createScanCrawlTask(ThrottledClient sruClient, Client jerseyClient,
            String centerRegistryUrl, int scanMaxDepth, List<EndpointOverrideConfig> endpointOverrides,
            EndpointFilter filter, ScanCrawlTaskCompletedCallback scanCrawlTaskCompletedCallback) {
        final ScanCrawlTask task = new ScanCrawlTask(sruClient, jerseyClient, centerRegistryUrl, scanMaxDepth,
                endpointOverrides, filter, scanCrawlTaskCompletedCallback);
        return task;
    }

    public static void scheduleScanCrawlTask(ScheduledExecutorService scheduler, ThrottledClient sruClient,
            Client jerseyClient, ScanCrawlParams params, EndpointFilter filter,
            ScanCrawlTaskCompletedCallback scanCrawlTaskCompletedCallback) {
        scheduleScanCrawlTask(scheduler, sruClient, jerseyClient,
                params.getCenterRegistryUrl(),
                params.getScanMaxDepth(),
                params.getEndpointOverrides(),
                filter, scanCrawlTaskCompletedCallback,
                params.getScanTaskInitialDelay(),
                params.getScanTaskInterval(),
                params.getScanTaskTimeUnit());
    }

    public static void scheduleScanCrawlTask(ScheduledExecutorService scheduler, ThrottledClient sruClient,
            Client jerseyClient, String centerRegistryUrl, int scanMaxDepth,
            List<EndpointOverrideConfig> endpointOverrides, EndpointFilter filter,
            ScanCrawlTaskCompletedCallback scanCrawlTaskCompletedCallback, long scanTaskInitialDelay,
            long scanTaskInterval, TimeUnit scanTaskTimeUnit) {
        final ScanCrawlTask task = createScanCrawlTask(sruClient, jerseyClient, centerRegistryUrl, scanMaxDepth,
                endpointOverrides, filter, scanCrawlTaskCompletedCallback);
        if (scanTaskInterval > 0) {
            log.debug("Scheduling Scan scrawl at regular intervals: {} {} with initial delay of {}", scanTaskInterval,
                    scanTaskTimeUnit, scanTaskInitialDelay);
            scheduler.scheduleAtFixedRate(task, scanTaskInitialDelay, scanTaskInterval, scanTaskTimeUnit);
        } else {
            log.debug("Submit Scan crawl task to run once after initial delay: {} {}", scanTaskInitialDelay,
                    scanTaskTimeUnit);
            scheduler.schedule(task, scanTaskInitialDelay, scanTaskTimeUnit);
        }
    }

    // ----------------------------------------------------------------------
    // search

    public static Search startSearch(ThrottledClient sruClient, Statistics stats,
            PerformLanguageDetectionCallback performLanguageDetectionCallback, SRUVersion version,
            List<Resource> resources, String queryType, String searchString, String searchLang, int startRecord,
            int maxRecords, final String userid) {
        if (resources == null || resources.isEmpty()) {
            // No resources
            return null;
        } else if (searchString == null || searchString.isEmpty()) {
            // No query
            return null;
        } else {
            return new Search(sruClient, performLanguageDetectionCallback, version, stats, resources, queryType,
                    searchString, searchLang, startRecord, maxRecords, userid);
        }
    }

    // ----------------------------------------------------------------------
    // life cycle

    protected static void shutdownAndAwaitTermination(ThrottledClient sruClient, ExecutorService scheduler,
            long executorShutdownTimeout) {
        try {
            sruClient.shutdown();
            scheduler.shutdown();
            Thread.sleep(executorShutdownTimeout);

            sruClient.shutdownNow();
            scheduler.shutdownNow();
            Thread.sleep(executorShutdownTimeout);
        } catch (InterruptedException ie) {
            sruClient.shutdownNow();
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}
