package eu.clarin.sru.fcs.aggregator.app;

import eu.clarin.sru.fcs.aggregator.cache.ScanCrawlTask;
import eu.clarin.sru.fcs.aggregator.cache.ScanCrawler;
import eu.clarin.sru.fcs.aggregator.cache.ScanCacheFiled;
import eu.clarin.sru.fcs.aggregator.cache.SimpleInMemScanCache;
import eu.clarin.sru.client.SRUThreadedClient;
import eu.clarin.sru.client.fcs.ClarinFCSRecordParser;
import eu.clarin.sru.fcs.aggregator.cache.EndpointUrlFilter;
import eu.clarin.sru.fcs.aggregator.sopt.CenterRegistryI;
import eu.clarin.sru.fcs.aggregator.sopt.CenterRegistryLive;
import eu.clarin.sru.fcs.aggregator.sopt.Languages;
import eu.clarin.sru.fcs.aggregator.cache.ScanCache;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import opennlp.tools.tokenize.TokenizerModel;
import org.joda.time.DateTime;
import org.zkoss.zk.ui.WebApp;
import org.zkoss.zk.ui.util.WebAppCleanup;
import org.zkoss.zk.ui.util.WebAppInit;

/**
 * Application initialization and clean up: only one SRU threaded client is used
 * in the application, it has to be shut down when the application stops. One
 * Languages object instance is used within the application.
 *
 * @author Yana Panchenko
 */
public class WebAppListener implements WebAppInit, WebAppCleanup {

    public static final String ACTIVE_SEARCH_CONTROLLERS = "ACTIVE_SEARCH_CONTROLLERS";
    public static final String SHARED_SRU_CLIENT = "SHARED_SRU_CLIENT";
    public static final String LANGUAGES = "LANG";
    public static final String CORPUS_CACHE = "CORPUS_CACHE";
    private static final Logger LOGGER = Logger.getLogger(WebAppListener.class.getName());
    //private static final int HOURS_BETWEEN_CACHE_UPDATE = 3;
    //private Timer cacheTimer;
    public static final String DE_TOK_MODEL = "/tokenizer/de-tuebadz-8.0-token.bin";
    private static final String AGGREGATOR_DIR_NAME = "aggregator";
    private static final String SCAN_DIR_NAME = "scan";
    private static final TimeUnit CACHE_UPDATE_INTERVAL_UNIT = TimeUnit.HOURS;
    private static final int CACHE_UPDATE_INTERVAL = 6;
    private static final int CACHE_MAX_DEPTH = 3;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    public static final String CORPUS_CRAWLER = "CORPUS_CRAWLER";

    @Override
    public void init(WebApp webapp) throws Exception {

        LOGGER.info("Aggregator is starting.");
        
        Set<SearchResults> activeControllers = new HashSet<SearchResults>();
        webapp.setAttribute(ACTIVE_SEARCH_CONTROLLERS, activeControllers);
        
        SRUThreadedClient sruClient = new SRUThreadedClient();
        sruClient.registerRecordParser(new ClarinFCSRecordParser());
        webapp.setAttribute(WebAppListener.SHARED_SRU_CLIENT, sruClient);

        Languages languages = new Languages();
        webapp.setAttribute(LANGUAGES, languages);
        
        setUpScanCache(webapp);
        //setUpScanCacheForReadOnly(webapp);
        
        setUpTokenizers(webapp);
        
    }

    @Override
    public void cleanup(WebApp webapp) throws Exception {
        LOGGER.info("Aggregator is shutting down.");
        Set<SearchResults> activeControllers = (Set<SearchResults>) webapp.getAttribute(ACTIVE_SEARCH_CONTROLLERS);
        for (SearchResults activeController : activeControllers) {
            activeController.shutdown();
        }
        SRUThreadedClient searchClient = (SRUThreadedClient) webapp.getAttribute(WebAppListener.SHARED_SRU_CLIENT);
        shutdownAndAwaitTermination(searchClient);
        shutdownAndAwaitTermination(scheduler);
        //cacheTimer.cancel();
    }

    private String getScanDirectory() {
        //File aggregatorDir = new File(System.getProperty("user.home"), "/." + AGGREGATOR_DIR_NAME);
        //File aggregatorDir = new File("/var/www", "/." + AGGREGATOR_DIR_NAME);
        File aggregatorDir = new File("/data/fcsAggregator");
        
        if (!aggregatorDir.exists()) {
            if (!aggregatorDir.mkdir()) {
                LOGGER.info("Scan directory does not exist and cannot be created: " 
                        + aggregatorDir.getAbsolutePath());
            }
        }
        File scanDir = new File(aggregatorDir, SCAN_DIR_NAME);
        if (!scanDir.exists()) {
            scanDir.mkdir();
        }
        String scanPath = scanDir.getAbsolutePath();
        LOGGER.info("Scan location: " + scanPath);
        return scanPath;
    }
    
    /**
     * Use this method instead of setUpScanCache() method if it is necessary
     * to run the application without scan data crawl, given that the scan 
     * data was crawled before and was stored as cache under appropriate
     * location (useful when testing or when smth is wrong with the endpoint 
     * scan responses).
     * 
     * @param webapp 
     */
    private void setUpScanCacheForReadOnly(WebApp webapp) {
        ScanCacheFiled scanCacheFiled = new ScanCacheFiled(getScanDirectory());
        ScanCache scanCache;
        LOGGER.info("Start cache read");
        try {
            scanCache = scanCacheFiled.read();
            LOGGER.info("Finished cache read, number of root corpora: " + scanCache.getRootCorpora().size());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while reading the scan cache!", e);
            scanCache = new SimpleInMemScanCache();
        }
        webapp.setAttribute(CORPUS_CACHE, scanCache);
    }

    private void setUpScanCache(WebApp webapp) {

        ScanCacheFiled scanCacheFiled = new ScanCacheFiled(getScanDirectory());
        CenterRegistryI centerRegistry = new CenterRegistryLive();
        SRUThreadedClient sruScanClient = (SRUThreadedClient) webapp.getAttribute(WebAppListener.SHARED_SRU_CLIENT);
        //EndpointUrlFilter filter = new EndpointUrlFilter();
        //filter.urlShouldContainAnyOf("leipzig");
        //filter.urlShouldContainAnyOf("uni-tuebingen.de");
        //filter.urlShouldContainAnyOf("uni-tuebingen.de", ".mpi.nl");
        //filter.urlShouldContainAnyOf("dspin.dwds.de", "lindat.");
        //ScanCrawler scanCrawler = new ScanCrawler(centerRegistry, sruScanClient, filter, CACHE_MAX_DEPTH);
        ScanCrawler scanCrawler = new ScanCrawler(centerRegistry, sruScanClient, null, CACHE_MAX_DEPTH);
        ScanCache scanCache;

        //synchronized (scanCrawler) {
            LOGGER.info("Start cache read");
            try {
                scanCache = scanCacheFiled.read();
                LOGGER.info("Finished cache read, number of root corpora: " + scanCache.getRootCorpora().size());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error while reading the scan cache!", e);
                scanCache = new SimpleInMemScanCache();
            }
        //}
        webapp.setAttribute(CORPUS_CACHE, scanCache);
        webapp.setAttribute(CORPUS_CRAWLER, scanCrawler);

        scheduler.scheduleAtFixedRate(
                new ScanCrawlTask(scanCrawler, scanCacheFiled, webapp),
                0, CACHE_UPDATE_INTERVAL, CACHE_UPDATE_INTERVAL_UNIT);

    }

    private void shutdownAndAwaitTermination(SRUThreadedClient sruClient) {
        // with shutdown() there are memory leaks when web app stops even if all requests have been processed;
        // with shutdownNow() there are memory leaks when web app stops only if not all requests have been processed
        //searchClient.shutdown();
        //searchClient.shutdownNow();
        try {
            sruClient.shutdown(); // Disable new tasks from being submitted
            // Wait 10 secs for existing tasks to terminate
            // replace with awaitTermination if ever provided in SRUClient API
            Thread.sleep(10000);
            sruClient.shutdownNow(); // Cancel currently executing tasks
            // Wait 10 secs for tasks to respond to being cancelled
            // replace with awaitTermination if ever provided in SRUClient API
            Thread.sleep(10000);
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            sruClient.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    private void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                    LOGGER.info("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    private void setUpTokenizers(WebApp webapp) {
        TokenizerModel model = null;
        try {
            InputStream tokenizerModelDeAsIS = this.getClass().getResourceAsStream(DE_TOK_MODEL);
            model = new TokenizerModel(tokenizerModelDeAsIS);
            tokenizerModelDeAsIS.close();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Failed to load tokenizer model", ex);
        }
        webapp.setAttribute(DE_TOK_MODEL, model);
    }

}
