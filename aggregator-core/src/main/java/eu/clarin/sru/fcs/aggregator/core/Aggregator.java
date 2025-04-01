package eu.clarin.sru.fcs.aggregator.core;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.client.Client;

import org.slf4j.LoggerFactory;

import eu.clarin.sru.client.SRUVersion;
import eu.clarin.sru.fcs.aggregator.client.ThrottledClient;
import eu.clarin.sru.fcs.aggregator.scan.EndpointFilter;
import eu.clarin.sru.fcs.aggregator.scan.Resource;
import eu.clarin.sru.fcs.aggregator.scan.ScanCrawlTask.ScanCrawlTaskCompletedCallback;
import eu.clarin.sru.fcs.aggregator.scan.Statistics;
import eu.clarin.sru.fcs.aggregator.search.PerformLanguageDetectionCallback;
import eu.clarin.sru.fcs.aggregator.search.Search;

public class Aggregator extends AggregatorBase {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Aggregator.class);

    // API stuff
    private ThrottledClient sruClient = null;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // callback to attach language to search results
    private AtomicReference<PerformLanguageDetectionCallback> performLanguageDetectionCallbackAtom = new AtomicReference<>(
            null);

    // search statistics
    private AtomicReference<Statistics> searchStatsAtom = new AtomicReference<>(new Statistics());

    // search task cache
    private final SearchCache activeSearches = new SearchCache();

    // ----------------------------------------------------------------------
    // life cycle

    public synchronized void init(Client jerseyClient, AggregatorParams params, EndpointFilter filter,
            ScanCrawlTaskCompletedCallback scanCrawlTaskCompletedCallback) {
        if (sruClient != null) {
            throw new IllegalStateException("Attempting to call init() after Aggregator has already been initialized!");
        }

        // create SRU client for scan/search requests
        sruClient = createClient(params);

        if (params.enableScanCrawlTask()) {
            // create and schedule scan crawl task
            scheduleScanCrawlTask(scheduler, sruClient, jerseyClient, params, filter, scanCrawlTaskCompletedCallback);
        }
    }

    public void scheduleScanCrawlTask(ThrottledClient sruClient, Client jerseyClient, ScanCrawlParams params,
            EndpointFilter filter, ScanCrawlTaskCompletedCallback scanCrawlTaskCompletedCallback) {
        scheduleScanCrawlTask(scheduler, sruClient, jerseyClient, params, filter, scanCrawlTaskCompletedCallback);
    }

    public void shutdown(ShutdownParams params) {
        shutdown(params.getExecutorShutdownTimeout());
    }

    public void shutdown(long executorShutdownTimeout) {
        // NOTE: multiple calls to shutdown should not matter

        log.info("Shutdown active searches.");
        shutdownSearches();

        log.info("Shutdown SRU client and scheduler.");
        shutdownAndAwaitTermination(sruClient, scheduler, executorShutdownTimeout);
    }

    protected void shutdownSearches() {
        for (Search search : activeSearches.getSearches().values()) {
            search.shutdown();
        }
    }

    // ----------------------------------------------------------------------
    // properties

    public PerformLanguageDetectionCallback getPerformLanguageDetectionCallback() {
        return performLanguageDetectionCallbackAtom.get();
    }

    public void setPerformLanguageDetectionCallback(PerformLanguageDetectionCallback performLanguageDetectionCallback) {
        performLanguageDetectionCallbackAtom.set(performLanguageDetectionCallback);
    }

    public Statistics getSearchStatistics() {
        return searchStatsAtom.get();
    }

    public void setSearchStatistics(Statistics stats) {
        searchStatsAtom.set(stats);
    }

    public void resetSearchStatistics() {
        searchStatsAtom.set(new Statistics());
    }

    // ----------------------------------------------------------------------
    // retrieve searches

    public Map<String, Search> getActiveSearches() {
        return activeSearches.getSearches();
    }

    public Search getSearchById(String id) {
        return activeSearches.getSearchById(id);
    }

    // ----------------------------------------------------------------------
    // start searches

    // this function should be thread-safe
    public Search startSearch(SRUVersion version, List<Resource> resources, String queryType, String searchString,
            String searchLang, int startRecord, int maxRecords) {
        if (sruClient == null) {
            throw new IllegalStateException("Aggregator has not been initialized yet! Call init() first!");
        }
        // NOTE: calls after shutdown will eventually throw in the clients executor ...

        final Statistics stats = searchStatsAtom.get();
        final PerformLanguageDetectionCallback performLanguageDetectionCallback = performLanguageDetectionCallbackAtom
                .get();

        final Search sr = startSearch(sruClient, stats, performLanguageDetectionCallback, version, resources, queryType,
                searchString, searchLang, startRecord, maxRecords);
        activeSearches.addSearch(sr);
        return sr;
    }

    public List<String> gcSearches(SearchGCParams params) {
        return gcSearches(params.getSearchesSizeThreshold(), params.getSearchesAgeThreshold());
    }

    public List<String> gcSearches(int searchesSizeThreshold, int searchesAgeThreshold) {
        return activeSearches.gc(searchesSizeThreshold, searchesAgeThreshold);
    }

}
