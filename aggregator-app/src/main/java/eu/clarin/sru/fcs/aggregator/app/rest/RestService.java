package eu.clarin.sru.fcs.aggregator.app.rest;

import java.io.IOException;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import eu.clarin.sru.client.SRUVersion;
import eu.clarin.sru.fcs.aggregator.app.AggregatorApp;
import eu.clarin.sru.fcs.aggregator.app.auth.AuthenticationInfo;
import eu.clarin.sru.fcs.aggregator.app.configuration.AggregatorConfiguration;
import eu.clarin.sru.fcs.aggregator.app.configuration.WeblichtConfig;
import eu.clarin.sru.fcs.aggregator.app.export.Exports;
import eu.clarin.sru.fcs.aggregator.app.export.WeblichtExportCache;
import eu.clarin.sru.fcs.aggregator.scan.FCSProtocolVersion;
import eu.clarin.sru.fcs.aggregator.scan.FCSSearchCapabilities;
import eu.clarin.sru.fcs.aggregator.scan.Resource;
import eu.clarin.sru.fcs.aggregator.scan.Resources;
import eu.clarin.sru.fcs.aggregator.scan.Statistics;
import eu.clarin.sru.fcs.aggregator.scan.Statistics.EndpointStats;
import eu.clarin.sru.fcs.aggregator.search.Result;
import eu.clarin.sru.fcs.aggregator.search.Search;
import eu.clarin.sru.fcs.aggregator.util.LanguagesISO693;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.links.Link;
import io.swagger.v3.oas.annotations.links.LinkParameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * The REST API of the Aggregator (actually, it's a HTTP API, not very restful).
 *
 * @author edima
 * @author ljo
 */
@Produces(MediaType.APPLICATION_JSON)
@Path("/rest")
@OpenAPIDefinition(info = @Info(title = AggregatorApp.NAME + " REST API", version = "2.0.0", description = "The "
        + AggregatorApp.NAME + " REST API is documented following the open API specification."
        + "<br />Code repository is hosted on <a href=\"https://github.com/clarin-eric/fcs-sru-aggregator\">GitHub</a>.", license = @License(name = "GNU General Public License Version 3 (GPLv3)", url = "https://www.gnu.org/licenses/gpl-3.0.txt")))
public class RestService {

    public static final String PARAM_QUERY = "query";
    public static final String PARAM_MODE = "mode";
    public static final String PARAM_AGGREGATION_CONTEXT = "x-aggregation-context";
    public static final String PARAM_CONSORTIA = "x-consortia";

    private static final String EXPORT_FILENAME_PREFIX = "ClarinFederatedContentSearch-";
    private static final String TCF_MEDIA_TYPE = "text/tcf+xml";
    private static final String ODS_MEDIA_TYPE = "application/vnd.oasis.opendocument.spreadsheet";
    private static final String EXCEL_MEDIA_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(RestService.class);
    private static final ObjectWriter ow = new ObjectMapper().writerWithDefaultPrettyPrinter();

    private static String toJson(Object o) throws JsonProcessingException {
        return ow.writeValueAsString(o);
    }

    // ----------------------------------------------------------------------

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Path("resources")
    @Operation(description = "Get all resources.", tags = { "web" }, responses = {
            @ApiResponse(description = "List of resource objects.", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Resource.class))) }) })
    public Response getResources(
            @Parameter(description = "Comma-separated list of consortia to filter institutions/centres, endpoints and finally resources", required = false) @QueryParam(PARAM_CONSORTIA) String consortiaRaw)
            throws IOException {
        List<Resource> resources = AggregatorApp.getInstance().getResources().getResources();

        if (consortiaRaw != null && !consortiaRaw.trim().isEmpty()) {
            List<String> consortia = Arrays.asList(consortiaRaw.replaceAll("\\s+", "").toUpperCase().split(","));
            resources = AggregatorApp.getInstance().getResources().getResourcesByConsortia(consortia);
        }

        return Response.ok(resources).build();
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Path("languages")
    @Operation(description = "Get all languages (code --> name) from all resources.", tags = { "web" }, responses = {
            @ApiResponse(description = "Mapping of language ISO code to English name.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LanguageMap.class), examples = @ExampleObject(value = "{\n  \"deu\": \"German\",\n  \"eng\": \"English\"}"))) })
    public Response getLanguages(
            @Parameter(description = "Comma-separated list of consortia to filter institutions/centres, endpoints and finally resources", required = false) @QueryParam(PARAM_CONSORTIA) String consortiaRaw)
            throws IOException {
        Set<String> codes = AggregatorApp.getInstance().getResources().getLanguages();

        if (consortiaRaw != null && !consortiaRaw.trim().isEmpty()) {
            List<String> consortia = Arrays.asList(consortiaRaw.replaceAll("\\s+", "").toUpperCase().split(","));
            codes = AggregatorApp.getInstance().getResources().getLanguagesByConsortia(consortia);
        }

        log.info("get language codes: {}", codes);
        Map<String, String> languages = LanguagesISO693.getInstance().getLanguageMap(codes);

        return Response.ok(languages).build();
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Path("consortia")
    @Operation(description = "Get all consorita from all resources.", tags = { "web" }, responses = {
            @ApiResponse(description = "List of consortia names.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ConsortiaList.class), examples = @ExampleObject(value = "[ null, \"CLARIN-D\", \"LINDAT/CLARIAH-CZ\" ]"))) })
    public Response getConsortia() throws IOException {
        Set<String> consortia = AggregatorApp.getInstance().getResources().getConsortia();
        return Response.ok(consortia).build();
    }

    @SuppressWarnings({ "unchecked" })
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Path("init")
    @Operation(description = "Get initial webpage data (resources, languages)."
            + " Optional 'x-aggregation-context' for pre-selecting resources "
            + "and/or 'mode' and 'query' for starting a search.", tags = { "web" }, responses = {
                    @ApiResponse(description = "Initial page data (resources, languages)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = InitData.class))) })
    public Response getInit(@Context final HttpServletRequest request,
            @Parameter(description = "Comma-separated list of consortia to filter institutions/centres, endpoints and finally resources", required = false) @QueryParam(PARAM_CONSORTIA) String consortiaRaw)
            throws IOException {
        log.info("get initial data");
        final Resources resources = AggregatorApp.getInstance().getResources();
        final Object query = request.getSession().getAttribute(PARAM_QUERY);
        final Object mode = request.getSession().getAttribute(PARAM_MODE);
        final Object contextString = request.getSession().getAttribute(PARAM_AGGREGATION_CONTEXT);
        final Object consortiaRawSession = request.getSession().getAttribute(PARAM_CONSORTIA);

        List<String> consortia = null;
        if (consortiaRawSession != null && consortiaRawSession instanceof String
                && !((String) consortiaRawSession).trim().isEmpty()) {
            consortia = Arrays.asList(((String) consortiaRawSession).replaceAll("\\s+", "").toUpperCase().split(","));
        }
        if (consortiaRaw != null && !consortiaRaw.trim().isEmpty()) {
            consortia = Arrays.asList(((String) consortiaRaw).replaceAll("\\s+", "").toUpperCase().split(","));
        }

        InitData data = new InitData();

        data.resources = resources.getResourcesByConsortia(consortia);
        data.languages = LanguagesISO693.getInstance().getLanguageMap(resources.getLanguagesByConsortia(consortia));
        data.weblichtLanguages = AggregatorApp.getInstance().getParams().getWeblichtConfig().getAcceptedTcfLanguages();

        if (query != null) {
            data.query = (String) query;
            request.getSession().setAttribute(PARAM_QUERY, null);
        }
        if (mode != null) {
            data.mode = (String) mode;
            request.getSession().setAttribute(PARAM_MODE, null);
        }
        if (contextString instanceof String) {
            Object context = new ObjectMapper().readValue((String) contextString, Object.class);
            data.aggregationContext = (Map<String, List<String>>) context; // preselected resources
            request.getSession().setAttribute(PARAM_AGGREGATION_CONTEXT, null);
        }

        return Response.ok(data).build();
    }

    @POST
    @Consumes({ MediaType.APPLICATION_FORM_URLENCODED })
    @Produces({ MediaType.APPLICATION_JSON })
    @Path("search")
    @Operation(description = "Start a new search.", responses = {
            @ApiResponse(responseCode = "201", description = "Started search, return search ID.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Long.class)), links = {
                    @Link(name = "Get search results", operationId = "getSearch", parameters = {
                            @LinkParameter(name = "id", expression = "$response.body") }),
                    @Link(name = "Load subSearch results", operationId = "postSearchNextResults", parameters = {
                            @LinkParameter(name = "id", expression = "$response.body") }) }),
            @ApiResponse(responseCode = "400", description = "Missing 'query' parameter."),
            @ApiResponse(responseCode = "400", description = "Missing 'resourceIds' parameter."),
            @ApiResponse(responseCode = "503", description = "No resources. Server might still be scanning. Please try again later."),
            @ApiResponse(responseCode = "500", description = "Initiating search failed.") }, tags = { "search" })
    public Response postSearch(
            @FormParam("query") String query,
            @FormParam("queryType") String queryType,
            @FormParam("firstResultIndex") Integer firstResultIndex,
            @FormParam("numberOfResults") Integer numberOfResults,
            @FormParam("language") String language,
            @FormParam("resourceIds[]") List<String> resourceIds,
            @Context final SecurityContext security) throws Exception {
        if (query == null || query.isEmpty()) {
            return Response.status(400).entity("'query' parameter expected").build();
        }
        // log.info("POST /search resourceIds: {}", resourceIds);
        if (resourceIds == null || resourceIds.isEmpty()) {
            return Response.status(400).entity("'resourceIds' parameter expected").build();
        }
        List<Resource> resources = AggregatorApp.getInstance().getResources()
                .getResourcesByIds(new HashSet<String>(resourceIds));
        // TODO: update for ClarinFCSConstants.QUERY_TYPE_FCS...
        if ("fcs".equals(queryType)) {
            List<Resource> tmp = new ArrayList<Resource>();
            for (Resource resource : resources) {
                if (resource.getEndpoint().getProtocol().equals(FCSProtocolVersion.VERSION_2) && resource.getEndpoint()
                        .getSearchCapabilities().contains(FCSSearchCapabilities.ADVANCED_SEARCH)) {
                    tmp.add(resource);
                }
            }
            resources = tmp;
        } else if ("lex".equals(queryType)) {
            List<Resource> tmp = new ArrayList<Resource>();
            for (Resource resource : resources) {
                if (resource.getEndpoint().getProtocol().equals(FCSProtocolVersion.VERSION_2)
                        && resource.getEndpoint().getSearchCapabilities().contains(FCSSearchCapabilities.LEX_SEARCH)) {
                    tmp.add(resource);
                }
            }
        }
        if (resources == null || resources.isEmpty()) {
            return Response.status(503).entity("No resources, please wait for the server to finish scanning").build();
        }

        if (firstResultIndex == null || firstResultIndex < 1) {
            firstResultIndex = 1;
        }
        if (firstResultIndex > 250) {
            firstResultIndex = 250;
        }

        if (numberOfResults == null || numberOfResults < 10) {
            numberOfResults = 10;
        }
        if (numberOfResults > 250) {
            numberOfResults = 250;
        }

        // TODO: move check to front?
        final AuthenticationInfo authInfo = AuthenticationInfo.fromPrincipal(security.getUserPrincipal());
        // TODO: check if resource selection all require auth, then fail early
        String userid = authInfo.isAuthenticated() ? authInfo.getUsername() : null;

        Search search = AggregatorApp.getInstance().startSearch(
                ("fcs".equals(queryType) || "lex".equals(queryType)) ? SRUVersion.VERSION_2_0 : null,
                resources, queryType, query, language, firstResultIndex, numberOfResults, userid);
        if (search == null) {
            return Response.status(500).entity("Initiating search failed").build();
        }
        URI uri = UriBuilder.fromMethod(RestService.class, "getSearch").resolveTemplate("id", search.getId()).build();
        return Response.created(uri).entity(toJson(search.getId())).build();
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Path("search/{id}")
    @Operation(description = "Get (partial) search result.", responses = {
            @ApiResponse(responseCode = "200", description = "Search result with (partial) responses and number of outstanding responses.", content = {
                    @Content(schema = @Schema(implementation = JsonSearch.class)) }),
            @ApiResponse(responseCode = "404", description = "Search job not found.") }, tags = { "search" })
    public Response getSearch(@PathParam("id") String searchId,
            @QueryParam("resourceId") String resourceId) throws Exception {
        // TODO: check if download of auth restricted resource?

        Search search = AggregatorApp.getInstance().getSearchById(searchId);
        if (search == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Search job not found").build();
        }

        JsonSearch js = new JsonSearch(search.getResults(resourceId));
        for (Result r : js.results) {
            if (r.getInProgress()) {
                js.inProgress++;
            }
        }
        return Response.ok(js).build();
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Path("search/{id}/metaonly")
    @Operation(description = "Get (partial) metadata-only search result.", responses = {
            @ApiResponse(responseCode = "200", description = "Search result with (partial) responses and number of outstanding responses.", content = {
                    @Content(schema = @Schema(implementation = JsonMetaOnlySearch.class)) }),
            @ApiResponse(responseCode = "404", description = "Search job not found.") }, tags = { "search" })
    public Response getSearchMetaOnly(@PathParam("id") String searchId,
            @QueryParam("resourceId") String resourceId) throws Exception {
        // TODO: check if download of auth restricted resource?

        Search search = AggregatorApp.getInstance().getSearchById(searchId);
        if (search == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Search job not found").build();
        }

        JsonSearch js = new JsonSearch(search.getResults(resourceId));
        for (Result r : js.results) {
            if (r.getInProgress()) {
                js.inProgress++;
            }
        }
        JsonMetaOnlySearch jmos = JsonMetaOnlySearch.fromJsonSearch(js);

        return Response.ok(jmos).build();
    }

    @POST
    @Consumes({ MediaType.APPLICATION_FORM_URLENCODED })
    @Produces({ MediaType.APPLICATION_JSON })
    @Path("search/{id}")
    @Operation(description = "Request more search results for a specific resource.", responses = {
            @ApiResponse(responseCode = "201", description = "Started subSearch, return search ID.", content = @Content(schema = @Schema(implementation = Long.class)), links = {
                    @Link(name = "Get search results", operationId = "getSearch", parameters = {
                            @LinkParameter(name = "id", expression = "$response.body") }) }),
            @ApiResponse(responseCode = "400", description = "Missing 'resourceId' parameter."),
            @ApiResponse(responseCode = "404", description = "Search job not found."),
            @ApiResponse(responseCode = "500", description = "Initiating subSearch failed.") }, tags = { "search" })
    public Response postSearchNextResults(@PathParam("id") String searchId,
            @FormParam("resourceId") String resourceId,
            @FormParam("numberOfResults") Integer numberOfResults,
            @Context final SecurityContext security) throws Exception {
        log.info("POST /search/{id}, searchId: {}, resourceId: {}", searchId, resourceId);
        if (resourceId == null || resourceId.isEmpty()) {
            return Response.status(400).entity("'resourceId' parameter expected").build();
        }
        Search search = AggregatorApp.getInstance().getSearchById(searchId);
        if (search == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Search job not found").build();
        }
        if (numberOfResults == null || numberOfResults < 10) {
            numberOfResults = 10;
        }
        if (numberOfResults > 250) {
            numberOfResults = 250;
        }

        // TODO: move check to front?
        // check if resource requires authentification info
        final AuthenticationInfo authInfo = AuthenticationInfo.fromPrincipal(security.getUserPrincipal());
        // TODO: check if resource requires auth, then fail early
        String userid = authInfo.isAuthenticated() ? authInfo.getUsername() : null;

        boolean ret = search.searchForNextResults(resourceId, numberOfResults, userid);
        if (ret == false) {
            return Response.status(500).entity("Initiating subSearch failed").build();
        }
        URI uri = UriBuilder.fromMethod(RestService.class, "getSearch").resolveTemplate("id", search.getId()).build();
        return Response.created(uri).entity(toJson(search.getId())).build();
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, TCF_MEDIA_TYPE, ODS_MEDIA_TYPE, EXCEL_MEDIA_TYPE })
    @Path("search/{id}/download")
    @Operation(description = "Download search results in various formats.", responses = {
            @ApiResponse(responseCode = "200", description = "Download: text", content = @Content(mediaType = MediaType.TEXT_PLAIN)),
            @ApiResponse(responseCode = "200", description = "Download: tcf", content = @Content(mediaType = TCF_MEDIA_TYPE)),
            @ApiResponse(responseCode = "200", description = "Download: ods", content = @Content(mediaType = ODS_MEDIA_TYPE)),
            @ApiResponse(responseCode = "200", description = "Download: excel", content = @Content(mediaType = EXCEL_MEDIA_TYPE)),
            @ApiResponse(responseCode = "200", description = "Download: csv", content = @Content(mediaType = MediaType.TEXT_PLAIN)),
            @ApiResponse(responseCode = "500", description = "Error while converting to the export format."),
            @ApiResponse(responseCode = "404", description = "Search job not found."),
            @ApiResponse(responseCode = "400", description = "format parameter must be one of: text, tcf, ods, excel, csv")
    }, tags = { "search" })
    public Response downloadSearchResults(@PathParam("id") String searchId,
            @QueryParam("resourceId") String resourceId,
            @QueryParam("filterLanguage") String filterLanguage,
            @QueryParam("format") String format) throws Exception {
        // TODO: check if download of auth restricted resource?

        Search search = AggregatorApp.getInstance().getSearchById(searchId);
        if (search == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Search job not found").build();
        }
        if (filterLanguage == null || filterLanguage.isEmpty()) {
            filterLanguage = null;
        }

        if (format == null || format.trim().isEmpty() || format.trim().equals("text")) {
            String text = Exports.getExportText(search.getResults(resourceId), filterLanguage);
            return download(text, MediaType.TEXT_PLAIN, search.getQuery() + ".txt");
        } else if (format.equals("tcf")) {
            byte[] bytes = Exports.getExportTCF(
                    search.getResults(resourceId), search.getSearchLanguage(), filterLanguage);
            return download(bytes, TCF_MEDIA_TYPE, search.getQuery() + ".xml");
        } else if (format.equals("ods")) {
            byte[] bytes = Exports.getExportODS(search.getResults(resourceId), filterLanguage);
            return download(bytes, ODS_MEDIA_TYPE, search.getQuery() + ".ods");
        } else if (format.equals("excel")) {
            byte[] bytes = Exports.getExportExcel(search.getResults(resourceId), filterLanguage);
            return download(bytes, EXCEL_MEDIA_TYPE, search.getQuery() + ".xls");
        } else if (format.equals("csv")) {
            String csv = Exports.getExportCSV(search.getResults(resourceId), filterLanguage, ";");
            return download(csv, MediaType.TEXT_PLAIN, search.getQuery() + ".csv");
        }

        return Response.status(Response.Status.BAD_REQUEST)
                .entity("format parameter must be one of: text, tcf, ods, excel, csv")
                .build();
    }

    Response download(Object entity, String mediaType, String filesuffix) {
        if (entity == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("error while converting to the export format").build();
        }
        String filename = EXPORT_FILENAME_PREFIX + filesuffix;
        return Response.ok(entity, mediaType)
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .build();
    }

    @GET
    @Produces({ TCF_MEDIA_TYPE })
    @Path("search/{id}/weblicht-export/{eid}")
    @Operation(description = "Get search result as TCF export (e.g. for Weblicht).", tags = {
            "weblicht" }, responses = {
                    @ApiResponse(responseCode = "200", description = "Download: tcf", content = @Content(mediaType = TCF_MEDIA_TYPE)),
                    @ApiResponse(responseCode = "500", description = "Error while converting to the export format."),
                    @ApiResponse(responseCode = "404", description = "Search job not found."),
                    @ApiResponse(responseCode = "404", description = "Weblicht export data not found.")
            })
    public Response getWeblichtExport(@PathParam("id") String searchId, @PathParam("eid") String exportId) {
        // TODO: check if download of auth restricted resource? or is tis allowed
        // always?

        WeblichtExportCache cache = AggregatorApp.getInstance().getWeblichtExportCacheBySearchId(searchId, false);
        if (cache == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Search job not found").build();
        }
        byte[] bytes = cache.getWeblichtExport(exportId);
        if (bytes == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Weblicht export data not found").build();
        }
        return download(bytes, TCF_MEDIA_TYPE,
                "export.{id}.{eid}.tcf".replace("{id}", searchId).replace("{eid}", exportId));
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Path("search/{id}/toWeblicht")
    @Operation(description = "Prepares search results for export to Weblicht and redirects to Weblicht.", tags = {
            "weblicht" }, responses = {
                    @ApiResponse(description = "Redirect to Weblicht page with query parameters set to access TCF encoded search results."),
                    @ApiResponse(responseCode = "404", description = "Search job not found."),
                    @ApiResponse(responseCode = "503", description = "Error while exporting to weblicht.") })
    public Response sendSearchResultsToWeblicht(@PathParam("id") String searchId,
            @QueryParam("filterLanguage") String filterLanguage,
            @QueryParam("resourceId") String resourceId) throws Exception {
        // TODO: check if download of auth restricted resource?

        Search search = AggregatorApp.getInstance().getSearchById(searchId);
        if (search == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Search job not found").build();
        }
        if (filterLanguage == null || filterLanguage.isEmpty()) {
            filterLanguage = null;
        }

        WeblichtConfig weblicht = AggregatorApp.getInstance().getParams().getWeblichtConfig();

        String url = null;
        byte[] bytes = Exports.getExportTCF(search.getResults(resourceId), search.getSearchLanguage(), filterLanguage);
        if (bytes != null) {
            // store bytes in-memory TCF weblicht export in searches (searches might only
            // live up to 60min if under high load)
            WeblichtExportCache cache = AggregatorApp.getInstance().getWeblichtExportCacheBySearchId(searchId, true);
            String exportId = cache.addWeblichtExport(bytes);

            url = "search/{id}/weblicht-export/{eid}".replace("{id}", searchId).replace("{eid}", exportId);
            url = weblicht.getExportServerUrl() + url;
            log.debug("Export weblicht url: {}", url);
        }

        URI weblichtUri = new URI(weblicht.getUrl() + url);
        return url == null
                ? Response.status(503).entity("error while exporting to weblicht").build()
                : Response.seeOther(weblichtUri).entity(weblichtUri).build();
    }

    @SuppressWarnings("serial")
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Path("statistics")
    @Operation(description = "Get scan and search statistics.", tags = { "web" }, responses = {
            @ApiResponse(description = "Scan and Search statistics for each endpoint.", content = @Content(schema = @Schema(implementation = ScanSearchStatisticsSchema.class))) })
    public Response getStatistics(
            @Parameter(description = "Comma-separated list of consortia to filter institutions/centres, endpoints and finally resources", required = false) @QueryParam(PARAM_CONSORTIA) String consortiaRaw)
            throws IOException {
        final Statistics scan = AggregatorApp.getInstance().getScanStatistics();
        final Statistics search = AggregatorApp.getInstance().getSearchStatistics();
        final AggregatorConfiguration.Params params = AggregatorApp.getInstance().getParams();

        Object info = new HashMap<String, Object>() {
            {
                put("last-scan", new HashMap<String, Object>() {
                    {
                        put("timeout", params.getENDPOINTS_SCAN_TIMEOUT_MS() / 1000.);
                        put("isScan", true);
                        put("institutions", scan.getInstitutions());
                        put("date", scan.getDate());
                    }
                });
                put("recent-searches", new HashMap<String, Object>() {
                    {
                        put("timeout", params.getENDPOINTS_SEARCH_TIMEOUT_MS() / 1000.);
                        put("isScan", false);
                        put("institutions", search.getInstitutions());
                        put("date", search.getDate());
                    }
                });
            }
        };

        if (consortiaRaw != null && !consortiaRaw.trim().isEmpty()) {
            List<String> consortia = Arrays.asList(consortiaRaw.replaceAll("\\s+", "").toUpperCase().split(","));
            List<Resource> resources = AggregatorApp.getInstance().getResources().getResourcesByConsortia(consortia);

            @SuppressWarnings("unchecked")
            Map<String, Object> statsScan = ((Map<String, Object>) ((Map<String, Object>) info).get("last-scan"));
            @SuppressWarnings("unchecked")
            Map<String, Map<String, EndpointStats>> institutionsScan = ((Map<String, Map<String, EndpointStats>>) statsScan
                    .get("institutions"));

            statsScan.put("institutions", filterInstitutionStatsByConsortia(institutionsScan, resources, consortia));

            @SuppressWarnings("unchecked")
            Map<String, Object> statsSearch = ((Map<String, Object>) ((Map<String, Object>) info)
                    .get("recent-searches"));
            @SuppressWarnings("unchecked")
            Map<String, Map<String, EndpointStats>> institutionsSearch = ((Map<String, Map<String, EndpointStats>>) statsSearch
                    .get("institutions"));

            statsSearch.put("institutions",
                    filterInstitutionStatsByConsortia(institutionsSearch, resources, consortia));
        }

        return Response.ok(info).build();
    }

    private Map<String, Map<String, EndpointStats>> filterInstitutionStatsByConsortia(
            Map<String, Map<String, EndpointStats>> institutions, List<Resource> resources, List<String> consortia) {
        return institutions.entrySet().stream()
                // check each endpoint of the institution
                // filter institutions that have at least one endpoint with at least on resource
                // where we can use the handle to find the Institution object to check the
                // consortium
                .filter(e -> e.getValue().entrySet().stream()
                        // filter all stats that have do have resources belonging to the consortium
                        .filter(e2 -> e2.getValue().getRootResources().stream()
                                // filter for resources belonging to the consortia
                                .filter(rr -> resources.stream()
                                        // check to retrieve the correct resource
                                        .filter(res -> res.getHandle().equals(rr.handle)
                                                && res.getEndpoint().getUrl().equals(e2.getKey())
                                                && res.getEndpointInstitution().getName().equals(e.getKey()))
                                        // check if in resource -> institution in consortia
                                        .filter(res -> consortia.contains(res.getEndpointInstitution().getConsortium()))
                                        // if we find at least one that we are happy
                                        // NOTE: assumption that all resources at this endpoint belong to the same
                                        // consortium anyway
                                        .findAny().isPresent())
                                // if at least one of the resources is valid then endpoint is OK
                                .findAny().isPresent())
                        // if at least one endpoint is valid then institution is OK
                        .findAny().isPresent())
                // only those institutions left that are in the consortia
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
    }

    @GET
    @Path("user")
    public Response getUser(@Context final SecurityContext security) throws IOException {
        log.debug("Authentication information requested. Security context {}.", security);
        final Principal userPrincipal = security.getUserPrincipal();
        log.debug("User principal: {}", userPrincipal);
        final AuthenticationInfo authInfo = AuthenticationInfo.fromPrincipal(userPrincipal);
        log.debug("Authentication info: {}", authInfo);
        return Response.ok(authInfo).build();
    }

}
