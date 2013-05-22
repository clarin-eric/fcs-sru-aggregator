package eu.clarin.sru.fcs.aggregator.app;

import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUThreadedClient;
import eu.clarin.sru.client.fcs.ClarinFCSRecordParser;
import eu.clarin.sru.fcs.aggregator.sparam2.Languages;
import eu.clarin.sru.fcs.aggregator.sresult.SearchResultsController;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.zkoss.zk.ui.WebApp;
import org.zkoss.zk.ui.util.WebAppCleanup;
import org.zkoss.zk.ui.util.WebAppInit;

/**
 *
 * @author Yana Panchenko
 */
public class WebAppListener implements WebAppInit, WebAppCleanup {

    public static String ACTIVE_SEARCH_CONTROLLERS = "ACTIVE_SEARCH_CONTROLLERS";
    public static String SHARED_SRU_CLIENT = "SHARED_SRU_CLIENT";
    public static String LANGUAGES = "LANG";
    
    private static final Logger logger = Logger.getLogger(WebAppListener.class.getName());

    @Override
    public void init(WebApp webapp) throws Exception {

        //int endOfClassesSharedPartOfName = WebAppListener.class.getPackage().getName().lastIndexOf(".");
        //Logger.getLogger(WebAppListener.class.getPackage().getName().substring(0, endOfClassesSharedPartOfName)).setLevel(Level.FINE);
        
        logger.info("Aggregator is starting...");
        
        Set<SearchResultsController> activeControllers = new HashSet<SearchResultsController>();
        webapp.setAttribute(ACTIVE_SEARCH_CONTROLLERS, activeControllers);

        SRUThreadedClient searchClient = new SRUThreadedClient();
        try {
            searchClient.registerRecordParser(new ClarinFCSRecordParser());
            webapp.setAttribute(WebAppListener.SHARED_SRU_CLIENT, searchClient);
        } catch (SRUClientException e) {
            logger.log(Level.SEVERE, "SRU Client Parser registration failed", e);
        }
        
        Languages languages = new Languages();
        webapp.setAttribute(LANGUAGES, languages);
    }

    @Override
    public void cleanup(WebApp webapp) throws Exception {
        logger.info("Aggregator is shutting down...");

        Set<SearchResultsController> activeControllers = (Set<SearchResultsController>) webapp.getAttribute(ACTIVE_SEARCH_CONTROLLERS);
        for (SearchResultsController activeController : activeControllers) {
            activeController.shutdown();
        }
        SRUThreadedClient searchClient = (SRUThreadedClient) webapp.getAttribute(WebAppListener.SHARED_SRU_CLIENT);

        // with shutdown() there are memory leaks when web app stops even if all requests have been processed;
        // with shutdownNow() there are memory leaks when web app stops only if not all requests have been processed
        searchClient.shutdownNow();
    }
}
