package eu.clarin.sru.fcs.aggregator.app;


import eu.clarin.sru.fcs.aggregator.sresult.SearchResultsController;
import java.util.logging.Logger;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.util.DesktopCleanup;

/**
 *
 * @author Yana Panchenko
 */
public class DesktopDestroyedListener implements DesktopCleanup {

    @Override
    public void cleanup(Desktop desktop) {

        Object recordsController = desktop.getAttribute(SearchResultsController.class.getSimpleName());
        if (recordsController != null) {
            Logger.getLogger(DesktopDestroyedListener.class.getName()).info("Cleaning up desktop");
            ((SearchResultsController) recordsController).shutdown();
        }
    }
}