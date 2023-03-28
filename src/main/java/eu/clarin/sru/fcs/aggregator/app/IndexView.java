package eu.clarin.sru.fcs.aggregator.app;

import eu.clarin.sru.fcs.aggregator.app.AggregatorConfiguration.Params.PiwikConfig;
import io.dropwizard.views.View;

/**
 * Dynamically generate {@code index.html} page.
 * 
 * @author ekoerner
 */
public class IndexView extends View {
    final PiwikConfig piwikConfig;

    protected IndexView(PiwikConfig config) {
        super("index.mustache");
        this.piwikConfig = config;
    }

    public PiwikConfig getPiwikConfig() {
        return piwikConfig;
    }

}
