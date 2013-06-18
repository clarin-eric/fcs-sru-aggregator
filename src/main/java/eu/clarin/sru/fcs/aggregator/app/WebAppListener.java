package eu.clarin.sru.fcs.aggregator.app;

import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUThreadedClient;
import eu.clarin.sru.client.fcs.ClarinFCSRecordParser;
import eu.clarin.sru.fcs.aggregator.sopt.Languages;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    public static String ACTIVE_SEARCH_CONTROLLERS = "ACTIVE_SEARCH_CONTROLLERS";
    public static String SHARED_SRU_CLIENT = "SHARED_SRU_CLIENT";
    public static String LANGUAGES = "LANG";
    
    private static final Logger LOGGER = Logger.getLogger(WebAppListener.class.getName());

    @Override
    public void init(WebApp webapp) throws Exception {
        LOGGER.info("Aggregator is starting.");
        Set<SearchResults> activeControllers = new HashSet<SearchResults>();
        webapp.setAttribute(ACTIVE_SEARCH_CONTROLLERS, activeControllers);
        SRUThreadedClient searchClient = new SRUThreadedClient();
        try {
            searchClient.registerRecordParser(new ClarinFCSRecordParser());
            webapp.setAttribute(WebAppListener.SHARED_SRU_CLIENT, searchClient);
        } catch (SRUClientException e) {
            LOGGER.log(Level.SEVERE, "SRU Client Parser registration failed", e);
        }
        
        Languages languages = new Languages();
        webapp.setAttribute(LANGUAGES, languages);
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
    }
}
