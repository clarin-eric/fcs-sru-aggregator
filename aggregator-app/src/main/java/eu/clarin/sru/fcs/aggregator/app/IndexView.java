package eu.clarin.sru.fcs.aggregator.app;

import eu.clarin.sru.fcs.aggregator.app.configuration.MatomoConfiguration;
import io.dropwizard.views.common.View;

/**
 * Dynamically generate {@code index.html} page.
 * 
 * @author ekoerner
 */
public class IndexView extends View {
    final MatomoConfiguration matomoConfig;
    final boolean showSearchResultLink;
    final String validatorUrl;
    final boolean authEnabled;
    final String username;
    final boolean weblichtEnabled;

    protected IndexView(MatomoConfiguration matomoConfig, boolean showSearchResultLink, String validatorUrl,
            boolean authEnabled, String username, boolean weblichtEnabled) {
        super("index.mustache");
        this.matomoConfig = matomoConfig;
        this.showSearchResultLink = showSearchResultLink;
        this.validatorUrl = validatorUrl;
        this.authEnabled = authEnabled;
        this.username = username;
        this.weblichtEnabled = weblichtEnabled;
    }

    public MatomoConfiguration getMatomoConfig() {
        return matomoConfig;
    }

}
