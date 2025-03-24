package eu.clarin.sru.fcs.aggregator.app;

import eu.clarin.sru.fcs.aggregator.app.AggregatorConfiguration.Params.PiwikConfig;
import io.dropwizard.views.common.View;

/**
 * Dynamically generate {@code index.html} page.
 * 
 * @author ekoerner
 */
public class IndexView extends View {
    final PiwikConfig piwikConfig;
    final boolean showSearchResultLink;
    final String validatorUrl;

    protected IndexView(PiwikConfig config, boolean showSearchResultLink, String validatorUrl) {
        super("index.mustache");
        this.piwikConfig = config;
        this.showSearchResultLink = showSearchResultLink;
        this.validatorUrl = validatorUrl;
    }

    public PiwikConfig getPiwikConfig() {
        return piwikConfig;
    }

}
