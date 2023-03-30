package eu.clarin.sru.fcs.aggregator.app;

import static eu.clarin.sru.fcs.aggregator.rest.RestService.PARAM_AGGREGATION_CONTEXT;
import static eu.clarin.sru.fcs.aggregator.rest.RestService.PARAM_MODE;
import static eu.clarin.sru.fcs.aggregator.rest.RestService.PARAM_QUERY;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.slf4j.LoggerFactory;

import eu.clarin.sru.fcs.aggregator.app.AggregatorConfiguration.Params.PiwikConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.links.Link;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * Simple jersey resource to dynamically generate {@code index.html} page.
 * 
 * @author ekoerner
 */
@Path("/")
public class IndexResource {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(IndexResource.class);

    PiwikConfig config;

    public IndexResource() {
        config = Aggregator.getInstance().getParams().getPiwikConfig();
    }

    @GET
    @Produces({ MediaType.TEXT_HTML })
    @Operation(description = "Start page of \"" + Aggregator.NAME + "\".", hidden = true)
    public IndexView getIndex() {
        return new IndexView(config);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_FORM_URLENCODED })
    @Produces({ MediaType.TEXT_HTML })
    @Operation(description = "Start external search request to either preselect resources for searching and/or set the search query and optionally start the search."
            + " Redirects to the Aggregator home page. Parameters will be stored in a session and queries on page load.", responses = {
                    @ApiResponse(description = "Returns default Aggregator 'index.html'.", links = {
                            @Link(name = "Initial JS page load", operationId = "getInit") }) }, tags = { "search" })
    public IndexView postExternalSearchRequest(@Context final HttpServletRequest request,
            @FormParam(PARAM_QUERY) String query, @FormParam(PARAM_MODE) String mode,
            @FormParam(PARAM_AGGREGATION_CONTEXT) String aggregationContext) {
        log.warn("Received external search request");

        if (query != null && !query.isEmpty()) {
            request.getSession().setAttribute(PARAM_QUERY, query);
            log.info("Param {}: {}", PARAM_QUERY, query);
        }
        if (mode != null && !mode.isEmpty()) {
            request.getSession().setAttribute(PARAM_MODE, mode);
            log.info("Param {}: {}", PARAM_MODE, mode);
        }

        if (aggregationContext != null && !aggregationContext.isEmpty()) {
            request.getSession().setAttribute(PARAM_AGGREGATION_CONTEXT, aggregationContext);
            log.info("Param {}: {}", PARAM_AGGREGATION_CONTEXT, aggregationContext);
        }

        return new IndexView(config);
    }
}
