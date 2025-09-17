package eu.clarin.sru.fcs.aggregator.app;

import static eu.clarin.sru.fcs.aggregator.app.rest.RestService.PARAM_AGGREGATION_CONTEXT;
import static eu.clarin.sru.fcs.aggregator.app.rest.RestService.PARAM_CONSORTIA;
import static eu.clarin.sru.fcs.aggregator.app.rest.RestService.PARAM_MODE;
import static eu.clarin.sru.fcs.aggregator.app.rest.RestService.PARAM_QUERY;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import org.slf4j.LoggerFactory;

import eu.clarin.sru.fcs.aggregator.app.auth.AuthenticationInfo;
import eu.clarin.sru.fcs.aggregator.app.configuration.PiwikConfig;
import eu.clarin.sru.fcs.aggregator.app.configuration.AggregatorConfiguration.Params;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

    final PiwikConfig config;
    final boolean searchResultLinkEnabled;
    final String validatorUrl;
    final boolean authEnabled;

    public IndexResource() {
        Params params = AggregatorApp.getInstance().getParams();

        config = params.getPiwikConfig();
        searchResultLinkEnabled = params.getSearchResultLinkEnabled();
        validatorUrl = params.getVALIDATOR_URL();
        authEnabled = params.getAAIConfig().isAAIEnabled();
    }

    @GET
    @Produces({ MediaType.TEXT_HTML })
    @Operation(description = "Start page of \"" + AggregatorApp.NAME + "\".", hidden = true)
    public IndexView getIndex(@Context final SecurityContext security) {
        final AuthenticationInfo authInfo = AuthenticationInfo.fromPrincipal(security.getUserPrincipal());
        final String username = authInfo.getDisplayName(); // TODO: what to show the user?
        return new IndexView(config, searchResultLinkEnabled, validatorUrl, authEnabled, username);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_FORM_URLENCODED })
    @Produces({ MediaType.TEXT_HTML })
    @Operation(description = "Start external search request to either preselect resources for searching and/or set the search query and optionally start the search."
            + " Redirects to the Aggregator home page. Parameters will be stored in a session and queries on page load.", responses = {
                    @ApiResponse(description = "Returns default Aggregator 'index.html'.", links = {
                            @Link(name = "Initial JS page load", operationId = "getInit") }) }, tags = { "search" })
    public IndexView postExternalSearchRequest(@Context final HttpServletRequest request,
            @Parameter(required = false) @FormParam(PARAM_QUERY) String query,
            @Parameter(required = false) @FormParam(PARAM_MODE) String mode,
            @Parameter(required = false) @FormParam(PARAM_AGGREGATION_CONTEXT) String aggregationContext,
            @Parameter(description = "Comma-separated list of consortia to filter institutions/centres, endpoints and finally resources", required = false) @FormParam(PARAM_CONSORTIA) String consortia,
            @Context final SecurityContext security) {
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

        if (consortia != null && !consortia.trim().isEmpty()) {
            request.getSession().setAttribute(PARAM_CONSORTIA, consortia);
            log.info("Param {}: {}", PARAM_CONSORTIA, consortia);
        }

        final AuthenticationInfo authInfo = AuthenticationInfo.fromPrincipal(security.getUserPrincipal());
        final String username = authInfo.getDisplayName(); // TODO: what to show the user?

        return new IndexView(config, searchResultLinkEnabled, validatorUrl, authEnabled, username);
    }
}
