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
    final boolean isLoggedIn;

    protected IndexView(PiwikConfig config, boolean showSearchResultLink, String validatorUrl, boolean isLoggedIn) {
        super("index.mustache");
        this.piwikConfig = config;
        this.showSearchResultLink = showSearchResultLink;
        this.validatorUrl = validatorUrl;
        this.isLoggedIn = isLoggedIn;
    }

    protected IndexView(PiwikConfig config, boolean showSearchResultLink, String validatorUrl) {
        this(config, showSearchResultLink, validatorUrl, false);
    }

    public PiwikConfig getPiwikConfig() {
        return piwikConfig;
    }

}
