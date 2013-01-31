package eu.clarin.sru.fcs.aggregator.app;


import eu.clarin.sru.fcs.aggregator.sresult.SearchResultsController;
import java.util.Set;
import java.util.logging.Logger;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.util.DesktopCleanup;

/**
 *
 * @author Yana Panchenko
 */
public class DesktopDestroyedListener implements DesktopCleanup {
    
    private static final Logger logger = Logger.getLogger(DesktopCleanup.class.getName());

    @Override
    public void cleanup(Desktop desktop) {
        logger.fine("Cleaning up desktop...");
        Object recordsController = desktop.getAttribute(SearchResultsController.class.getSimpleName());
        Set<SearchResultsController> activeControllers = (Set<SearchResultsController>) desktop.getWebApp().getAttribute(WebAppListener.ACTIVE_SEARCH_CONTROLLERS);
        if (recordsController != null) {
            SearchResultsController srController = (SearchResultsController) recordsController;
            srController.shutdown();
            activeControllers.remove(srController);
        }
    }
}