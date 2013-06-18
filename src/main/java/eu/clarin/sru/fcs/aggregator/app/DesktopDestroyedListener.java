package eu.clarin.sru.fcs.aggregator.app;


import java.util.Set;
import java.util.logging.Logger;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.util.DesktopCleanup;

/**
 * Desktop cleanup implementation. Closes Search Results page shutting down its
 * corresponding connections/threads.
 * 
 * @author Yana Panchenko
 */
public class DesktopDestroyedListener implements DesktopCleanup {
    
    private static final Logger LOGGER = Logger.getLogger(DesktopDestroyedListener.class.getName());

    @Override
    public void cleanup(Desktop desktop) {
        LOGGER.info("Cleaning up desktop.");
        Object recordsController = desktop.getAttribute(SearchResults.class.getSimpleName());
        Set<SearchResults> activeControllers = (Set<SearchResults>) desktop.getWebApp().getAttribute(WebAppListener.ACTIVE_SEARCH_CONTROLLERS);
        if (recordsController != null) {
            SearchResults srController = (SearchResults) recordsController;
            srController.shutdown();
            activeControllers.remove(srController);
            LOGGER.info("Shutting down Search Results.");
        }
    }
}