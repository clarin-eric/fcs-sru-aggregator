package eu.clarin.sru.fcs.aggregator.app;


import java.util.logging.Logger;
import org.zkoss.zk.ui.WebApp;
import org.zkoss.zk.ui.util.WebAppCleanup;

/**
 *
 * @author Yana Panchenko
 */
public class WebAppDestroyedListener implements WebAppCleanup {

    @Override
    public void cleanup(WebApp wapp) throws Exception {
        //TODO
        Logger.getLogger(WebAppDestroyedListener.class.getName()).info("TODO: WebAppDestroyedListener");
    }
}