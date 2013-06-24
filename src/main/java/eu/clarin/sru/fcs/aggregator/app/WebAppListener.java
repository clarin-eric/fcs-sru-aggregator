package eu.clarin.sru.fcs.aggregator.app;

import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUThreadedClient;
import eu.clarin.sru.client.fcs.ClarinFCSRecordParser;
import eu.clarin.sru.fcs.aggregator.sopt.CorporaScanCache;
import eu.clarin.sru.fcs.aggregator.sopt.CorpusCache;
import eu.clarin.sru.fcs.aggregator.sopt.Languages;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    private static final int HOURS_BETWEEN_CACHE_UPDATE = 3;
    
    private Timer cacheTimer;

    @Override
    public void init(WebApp webapp) throws Exception {
        LOGGER.info("Aggregator is starting.");
        Set<SearchResults> activeControllers = new HashSet<SearchResults>();
        webapp.setAttribute(ACTIVE_SEARCH_CONTROLLERS, activeControllers);
        SRUThreadedClient searchClient = new SRUThreadedClient();
        searchClient.registerRecordParser(new ClarinFCSRecordParser());
        webapp.setAttribute(WebAppListener.SHARED_SRU_CLIENT, searchClient);
        
        Languages languages = new Languages();
        webapp.setAttribute(LANGUAGES, languages);
        
        // set up timer to run the cache corpora scan info task
        //cacheTimer = new Timer();
        //CorpusCache cache = new CorpusCache();
        //webapp.setAttribute(CORPUS_CACHE, cache);
        
        //DateTime date = new DateTime();
        //date = date.withHourOfDay(1);
        //date = date.withMinuteOfHour(0);
        //date = date.withSecondOfMinute(0);
        //if (date.isBeforeNow()) {
        //    date = date.plusDays(1);
        //}
        //LOGGER.info(date.toLocalTime().toString() + " " + date.toLocalTime().toString());
        //cacheTimer.scheduleAtFixedRate(new CacheCorporaScanTask(cache, searchClient), date.toDate(), HOURS_BETWEEN_CACHE_UPDATE * 3600000);
        
        // read cache from file
        CorporaScanCache cache = new CorporaScanCache(webapp.getRealPath("scan") + "/");
        webapp.setAttribute(CORPUS_CACHE, cache);
    }

    @Override
    public void cleanup(WebApp webapp) throws Exception {
        LOGGER.info("Aggregator is shutting down.");
        Set<SearchResults> activeControllers = (Set<SearchResults>) webapp.getAttribute(ACTIVE_SEARCH_CONTROLLERS);
        for (SearchResults activeController : activeControllers) {
            activeController.shutdown();
        }
        SRUThreadedClient searchClient = (SRUThreadedClient) webapp.getAttribute(WebAppListener.SHARED_SRU_CLIENT);
        // with shutdown() there are memory leaks when web app stops even if all requests have been processed;
        // with shutdownNow() there are memory leaks when web app stops only if not all requests have been processed
        searchClient.shutdownNow();
        //cacheTimer.cancel();
    }
}
