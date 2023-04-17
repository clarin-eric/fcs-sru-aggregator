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
    final boolean showSearchResultLink;

    protected IndexView(PiwikConfig config, boolean showSearchResultLink) {
        super("index.mustache");
        this.piwikConfig = config;
        this.showSearchResultLink = showSearchResultLink;
    }

    public PiwikConfig getPiwikConfig() {
        return piwikConfig;
    }

}
