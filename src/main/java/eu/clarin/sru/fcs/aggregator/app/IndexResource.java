package eu.clarin.sru.fcs.aggregator.app;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import eu.clarin.sru.fcs.aggregator.app.AggregatorConfiguration.Params.PiwikConfig;

/**
 * Simple jersey resource to dynamically generate {@code index.html} page.
 * 
 * @author ekoerner
 */
@Path("/")
public class IndexResource {
    PiwikConfig config;

    public IndexResource() {
        config = Aggregator.getInstance().getParams().getPiwikConfig();
    }

    @GET
    public IndexView getIndex() {
        return new IndexView(config);
    }
}
