package eu.clarin.sru.fcs.aggregator.core;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.client.Client;

import org.slf4j.LoggerFactory;

import eu.clarin.sru.client.SRUThreadedClient;
import eu.clarin.sru.client.SRUVersion;
import eu.clarin.sru.client.fcs.ClarinFCSClientBuilder;
import eu.clarin.sru.client.fcs.ClarinFCSEndpointDescriptionParser;
import eu.clarin.sru.fcs.aggregator.client.MaxConcurrentRequestsCallback;
import eu.clarin.sru.fcs.aggregator.client.ThrottledClient;
import eu.clarin.sru.fcs.aggregator.scan.EndpointConfig;
import eu.clarin.sru.fcs.aggregator.scan.EndpointFilter;
import eu.clarin.sru.fcs.aggregator.scan.Resource;
import eu.clarin.sru.fcs.aggregator.scan.ScanCrawlTask;
import eu.clarin.sru.fcs.aggregator.scan.Statistics;
import eu.clarin.sru.fcs.aggregator.scan.ScanCrawlTask.ScanCrawlTaskCompletedCallback;
import eu.clarin.sru.fcs.aggregator.search.PerformLanguageDetectionCallback;
import eu.clarin.sru.fcs.aggregator.search.Search;

public class Aggregator {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Aggregator.class);

    // API stuff
    private ThrottledClient sruClient = null;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private PerformLanguageDetectionCallback performLanguageDetectionCallback;

    // statistics
    private AtomicReference<Statistics> searchStatsAtom = new AtomicReference<Statistics>(new Statistics());

    // search task cache
    private Map<String, Search> activeSearches = Collections.synchronizedMap(new HashMap<>());

    // ----------------------------------------------------------------------
    // life cycle

    public void init(Client jerseyClient, AggregatorParams params, EndpointFilter filter,
            ScanCrawlTaskCompletedCallback scanCrawlTaskCompletedCallback) {
        // create SRU client for scan/search requests
        sruClient = createClient(params);

        // create and schedule scan crawl task
        scheduleScanCrawlTask(sruClient, jerseyClient, params, filter, scanCrawlTaskCompletedCallback);
    }

    public void scheduleScanCrawlTask(ThrottledClient sruClient, Client jerseyClient, ScanCrawlParams params,
            EndpointFilter filter, ScanCrawlTaskCompletedCallback scanCrawlTaskCompletedCallback) {
        scheduleScanCrawlTask(sruClient, jerseyClient,
                params.getCenterRegistryUrl(),
                params.getScanMaxDepth(),
                params.getAdditionalCQLEndpoints(),
                params.getAdditionalFCSEndpoints(),
                filter, scanCrawlTaskCompletedCallback,
                params.getScanTaskInitialDelay(),
                params.getScanTaskInterval(),
                params.getScanTaskTimeUnit());
    }

    public void scheduleScanCrawlTask(ThrottledClient sruClient, Client jerseyClient,
            String centerRegistryUrl, int scanMaxDepth, List<EndpointConfig> additionalCQLEndpoints,
            List<EndpointConfig> additionalFCSEndpoints, EndpointFilter filter,
            ScanCrawlTaskCompletedCallback scanCrawlTaskCompletedCallback, long scanTaskInitialDelay,
            long scanTaskInterval, TimeUnit scanTaskTimeUnit) {
        final ScanCrawlTask task = createScanCrawlTask(sruClient, jerseyClient, centerRegistryUrl, scanMaxDepth,
                additionalCQLEndpoints, additionalFCSEndpoints, filter, scanCrawlTaskCompletedCallback);
        scheduler.scheduleAtFixedRate(task, scanTaskInitialDelay, scanTaskInterval, scanTaskTimeUnit);
    }

    public void shutdown(ShutdownParams params) {
        shutdown(params.getExecutorShutdownTimeout());
    }

    public void shutdown(long executorShutdownTimeout) {
        log.info("Shutdown active searches.");
        shutdownSearches();

        log.info("Shutdown SRU client and scheduler.");
        shutdownAndAwaitTermination(executorShutdownTimeout, sruClient, scheduler);
    }

    protected void shutdownSearches() {
        for (Search search : activeSearches.values()) {
            search.shutdown();
        }
    }

    protected static void shutdownAndAwaitTermination(long executorShutdownTimeout, ThrottledClient sruClient,
            ExecutorService scheduler) {
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

    // ----------------------------------------------------------------------
    // properties

    public PerformLanguageDetectionCallback getPerformLanguageDetectionCallback() {
        return performLanguageDetectionCallback;
    }

    public void setPerformLanguageDetectionCallback(PerformLanguageDetectionCallback performLanguageDetectionCallback) {
        this.performLanguageDetectionCallback = performLanguageDetectionCallback;
    }

    public Statistics getSearchStatistics() {
        return searchStatsAtom.get();
    }

    public void setSearchStatistics(Statistics stats) {
        searchStatsAtom.set(stats);
    }

    // ----------------------------------------------------------------------
    // retrieve searches

    public Map<String, Search> getActiveSearches() {
        return activeSearches;
    }

    public Search getSearchById(String id) {
        return activeSearches.get(id);
    }

    // ----------------------------------------------------------------------
    // start searches

    // this function should be thread-safe
    public Search startSearch(SRUVersion version, List<Resource> resources, String queryType, String searchString,
            String searchLang, int firstRecord, int maxRecords) throws Exception {
        if (resources.isEmpty()) {
            // No resources
            return null;
        } else if (searchString.isEmpty()) {
            // No query
            return null;
        } else {
            final Statistics stats = searchStatsAtom.get();
            final Search sr = new Search(sruClient, performLanguageDetectionCallback, version, stats, resources,
                    queryType, searchString, searchLang, maxRecords);
            activeSearches.put(sr.getId(), sr);
            return sr;
        }
    }

    public List<String> gcSearches(SearchGCParams params) {
        return gcSearches(params.getSearchesSizeThreshold(), params.getSearchesAgeThreshold());
    }

    public List<String> gcSearches(int searchesSizeThreshold, int searchesAgeThreshold) {
        List<String> toBeRemoved = new ArrayList<>();
        if (activeSearches.size() > searchesSizeThreshold) {
            long t0 = System.currentTimeMillis();
            for (Map.Entry<String, Search> e : activeSearches.entrySet()) {
                long dtmin = (t0 - e.getValue().getCreatedAt()) / 1000 / 60;
                if (dtmin > searchesAgeThreshold) {
                    log.info("removing search {}: {} minutes old", e.getKey(), dtmin);
                    toBeRemoved.add(e.getKey());
                }
            }
            for (String searchId : toBeRemoved) {
                activeSearches.remove(searchId);
            }
        }
        return toBeRemoved;
    }

    // ----------------------------------------------------------------------
    // creators

    public static ThrottledClient createClient(SRUFCSClientParams params) {
        return createClient(params.getEndpointScanTimeout(),
                params.getEndpointSearchTimeout(),
                params.getMaxConcurrentScanRequestsPerEndpoint(),
                params.getMaxConcurrentSearchRequestsPerEndpoint(),
                params.getMaxConcurrentSearchRequestsPerSlowEndpoint(),
                params.getSlowEndpoints());
    }

    public static ThrottledClient createClient(int endpointScanTimeout, int endpointSearchTimeout,
            int maxConcurrentScanRequestsPerEndpoint,
            int maxConcurrentSearchRequestsPerEndpoint, int maxConcurrentSearchRequestsPerSlowEndpoint,
            List<URI> slowEndpoints) {

        final SRUThreadedClient sruScanClient = new ClarinFCSClientBuilder()
                .setConnectTimeout(endpointScanTimeout)
                .setSocketTimeout(endpointScanTimeout)
                .addDefaultDataViewParsers()
                .registerExtraResponseDataParser(
                        new ClarinFCSEndpointDescriptionParser())
                .enableLegacySupport()
                .buildThreadedClient();

        final SRUThreadedClient sruSearchClient = new ClarinFCSClientBuilder()
                .setConnectTimeout(endpointSearchTimeout)
                .setSocketTimeout(endpointSearchTimeout)
                .addDefaultDataViewParsers()
                .registerExtraResponseDataParser(
                        new ClarinFCSEndpointDescriptionParser())
                .enableLegacySupport()
                .buildThreadedClient();

        final MaxConcurrentRequestsCallback maxScanConcurrentRequestsCallback = new MaxConcurrentRequestsCallback() {
            @Override
            public int getMaxConcurrentRequest(URI baseURI) {
                return maxConcurrentScanRequestsPerEndpoint;
            }
        };

        final MaxConcurrentRequestsCallback maxSearchConcurrentRequestsCallback = new MaxConcurrentRequestsCallback() {
            @Override
            public int getMaxConcurrentRequest(URI baseURI) {
                return (slowEndpoints != null && slowEndpoints.contains(baseURI))
                        ? maxConcurrentSearchRequestsPerSlowEndpoint
                        : maxConcurrentSearchRequestsPerEndpoint;
            }
        };

        final ThrottledClient sruClient = new ThrottledClient(
                sruScanClient, maxScanConcurrentRequestsCallback,
                sruSearchClient, maxSearchConcurrentRequestsCallback);

        return sruClient;
    }

    public static ScanCrawlTask createScanCrawlTask(ThrottledClient sruClient, Client jerseyClient,
            ScanCrawlTaskParams params, EndpointFilter filter,
            ScanCrawlTaskCompletedCallback scanCrawlTaskCompletedCallback) {
        return createScanCrawlTask(sruClient, jerseyClient,
                params.getCenterRegistryUrl(),
                params.getScanMaxDepth(),
                params.getAdditionalCQLEndpoints(),
                params.getAdditionalFCSEndpoints(),
                filter, scanCrawlTaskCompletedCallback);
    }

    public static ScanCrawlTask createScanCrawlTask(ThrottledClient sruClient, Client jerseyClient,
            String centerRegistryUrl, int scanMaxDepth, List<EndpointConfig> additionalCQLEndpoints,
            List<EndpointConfig> additionalFCSEndpoints, EndpointFilter filter,
            ScanCrawlTaskCompletedCallback scanCrawlTaskCompletedCallback) {
        final ScanCrawlTask task = new ScanCrawlTask(sruClient, jerseyClient, centerRegistryUrl, scanMaxDepth,
                additionalCQLEndpoints, additionalFCSEndpoints, filter, scanCrawlTaskCompletedCallback);
        return task;
    }

}
