package eu.clarin.sru.fcs.aggregator.app;

import eu.clarin.sru.fcs.aggregator.app.configuration.PiwikConfig;
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
    final boolean authEnabled;
    final String username;

    protected IndexView(PiwikConfig config, boolean showSearchResultLink, String validatorUrl, boolean authEnabled,
            String username) {
        super("index.mustache");
        this.piwikConfig = config;
        this.showSearchResultLink = showSearchResultLink;
        this.validatorUrl = validatorUrl;
        this.authEnabled = authEnabled;
        this.username = username;
    }

    public PiwikConfig getPiwikConfig() {
        return piwikConfig;
    }

}
