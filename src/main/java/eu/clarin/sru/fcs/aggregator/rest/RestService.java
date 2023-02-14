package eu.clarin.sru.fcs.aggregator.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import eu.clarin.sru.client.SRUVersion;
import eu.clarin.sru.fcs.aggregator.app.Aggregator;
import eu.clarin.sru.fcs.aggregator.app.AggregatorConfiguration;
import eu.clarin.sru.fcs.aggregator.app.AggregatorConfiguration.Params.WeblichtConfig;
import static eu.clarin.sru.fcs.aggregator.app.ExternalSearchRequestForwardingFilter.PARAM_AGGREGATION_CONTEXT;
import static eu.clarin.sru.fcs.aggregator.app.ExternalSearchRequestForwardingFilter.PARAM_MODE;
import static eu.clarin.sru.fcs.aggregator.app.ExternalSearchRequestForwardingFilter.PARAM_QUERY;
import eu.clarin.sru.fcs.aggregator.scan.Corpora;
import eu.clarin.sru.fcs.aggregator.scan.Corpus;
import eu.clarin.sru.fcs.aggregator.scan.FCSProtocolVersion;
import eu.clarin.sru.fcs.aggregator.scan.FCSSearchCapabilities;
import eu.clarin.sru.fcs.aggregator.scan.Statistics;
import eu.clarin.sru.fcs.aggregator.search.Result;
import eu.clarin.sru.fcs.aggregator.search.Search;
import eu.clarin.sru.fcs.aggregator.util.LanguagesISO693;
import eu.clarin.sru.fcs.aggregator.search.Exports;
import io.dropwizard.setup.Environment;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
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
import org.slf4j.LoggerFactory;

/**
 * The REST API of the Aggregator (actually, it's a HTTP API, not very restful).
 *
 * @author edima
 * @author ljo
 */
@Produces(MediaType.APPLICATION_JSON)
@Path("/")
public class RestService {

    private static final String EXPORT_FILENAME_PREFIX = "ClarinFederatedContentSearch-";
    private static final String TCF_MEDIA_TYPE = "text/tcf+xml";
    private static final String ODS_MEDIA_TYPE = "application/vnd.oasis.opendocument.spreadsheet";
    private static final String EXCEL_MEDIA_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String SEARCH_RESULTS_ENCODING = "UTF-8";

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(RestService.class);

    ObjectWriter ow = new ObjectMapper().writerWithDefaultPrettyPrinter();

    @Context
    HttpServletRequest request;
    @Context
    ServletContext servletContext;

    private final Environment environment;

    public RestService(Environment environment) {
        this.environment = environment;
    }

    private String toJson(Object o) throws JsonProcessingException {
        return ow.writeValueAsString(o);
    }

    @GET
    @Path("corpora")
    public Response getCorpora() throws IOException {
        List<Corpus> corpora = Aggregator.getInstance().getCorpora().getCorpora();
        return Response.ok(toJson(corpora)).build();
    }

    @GET
    @Path("languages")
    public Response getLanguages() throws IOException {
        Set<String> codes = Aggregator.getInstance().getCorpora().getLanguages();
        log.info("get language codes: {}", codes);
        Map<String, String> languages = LanguagesISO693.getInstance().getLanguageMap(codes);
        return Response.ok(toJson(languages)).build();
    }

    @GET
    @Path("init")
    public Response getInit(@Context final HttpServletRequest request) throws IOException {
        log.info("get initial data");
        final Corpora corpora = Aggregator.getInstance().getCorpora();
        final Object query = request.getSession().getAttribute(PARAM_QUERY);
        final Object mode = request.getSession().getAttribute(PARAM_MODE);
        final Object contextString = request.getSession().getAttribute(PARAM_AGGREGATION_CONTEXT);
        Object j = new HashMap<String, Object>() {
            {
                if (query != null) {
                    put(PARAM_QUERY, query);
                    request.getSession().setAttribute(PARAM_QUERY, null);
                }
                if (mode != null) {
                    put(PARAM_MODE, mode);
                    request.getSession().setAttribute(PARAM_MODE, null);
                }
                if (contextString instanceof String) {
                    Object context = new ObjectMapper().readValue((String) contextString, Object.class);
                    put(PARAM_AGGREGATION_CONTEXT, context); // preselected corpora
                    request.getSession().setAttribute(PARAM_AGGREGATION_CONTEXT, null);
                }
                put("corpora", corpora.getCorpora());
                put("languages", LanguagesISO693.getInstance().getLanguageMap(corpora.getLanguages()));
                put("weblichtLanguages",
                        Aggregator.getInstance().getParams().getWeblichtConfig().getAcceptedTcfLanguages());
            }
        };
        return Response.ok(toJson(j)).build();
    }

    @POST
    @Path("search")
    public Response postSearch(
            @FormParam("query") String query,
            @FormParam("queryType") String queryType,
            @FormParam("firstResultIndex") Integer firstResultIndex,
            @FormParam("numberOfResults") Integer numberOfResults,
            @FormParam("language") String language,
            @FormParam("corporaIds[]") List<String> corporaIds) throws Exception {
        if (query == null || query.isEmpty()) {
            return Response.status(400).entity("'query' parameter expected").build();
        }
        // log.info("POST /search corporaIds: {}", corporaIds);
        if (corporaIds == null || corporaIds.isEmpty()) {
            return Response.status(400).entity("'corporaIds' parameter expected").build();
        }
        List<Corpus> corpora = Aggregator.getInstance().getCorpora().getCorporaByIds(new HashSet<String>(corporaIds));
        if ("fcs".equals(queryType)) {
            List<Corpus> tmp = new ArrayList<Corpus>();
            for (Corpus corpus : corpora) {
                if (corpus.getEndpoint().getProtocol().equals(FCSProtocolVersion.VERSION_2) && corpus.getEndpoint()
                        .getSearchCapabilities().contains(FCSSearchCapabilities.ADVANCED_SEARCH)) {
                    tmp.add(corpus);
                }
            }
            corpora = tmp;
        }
        if (corpora == null || corpora.isEmpty()) {
            return Response.status(503).entity("No corpora, please wait for the server to finish scanning").build();
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
        Search search = Aggregator.getInstance().startSearch(
                "fcs".equals(queryType) ? SRUVersion.VERSION_2_0 : SRUVersion.VERSION_1_2,
                corpora, queryType, query, language, firstResultIndex, numberOfResults);
        if (search == null) {
            return Response.status(500).entity("Initiating search failed").build();
        }
        URI uri = URI.create("" + search.getId());
        return Response.created(uri).entity(uri).build();
    }

    public static class JsonSearch {

        @JsonProperty
        int inProgress = 0;
        @JsonProperty
        List<Result> results;

        public JsonSearch(List<Result> results) {
            this.results = results;
        }
    }

    @GET
    @Path("search/{id}")
    public Response getSearch(@PathParam("id") Long searchId,
            @QueryParam("corpusId") String corpusId) throws Exception {
        Search search = Aggregator.getInstance().getSearchById(searchId);
        if (search == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Search job not found").build();
        }

        JsonSearch js = new JsonSearch(search.getResults(corpusId));
        for (Result r : js.results) {
            if (r.getInProgress()) {
                js.inProgress++;
            }
        }
        return Response.ok(js).build();
    }

    @POST
    @Path("search/{id}")
    public Response postSearchNextResults(@PathParam("id") Long searchId,
            @FormParam("corpusId") String corpusId,
            @FormParam("numberOfResults") Integer numberOfResults) throws Exception {
        log.info("POST /search/{id}, searchId: {}, corpusId: {}", searchId, corpusId);
        if (corpusId == null || corpusId.isEmpty()) {
            return Response.status(400).entity("'corpusId' parameter expected").build();
        }
        Search search = Aggregator.getInstance().getSearchById(searchId);
        if (search == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Search job not found").build();
        }
        if (numberOfResults == null || numberOfResults < 10) {
            numberOfResults = 10;
        }
        if (numberOfResults > 250) {
            numberOfResults = 250;
        }

        boolean ret = search.searchForNextResults(corpusId, numberOfResults);
        if (ret == false) {
            return Response.status(500).entity("Initiating subSearch failed").build();
        }
        URI uri = URI.create("" + search.getId());
        return Response.created(uri).entity(uri).build();
    }

    @GET
    @Path("search/{id}/download")
    public Response downloadSearchResults(@PathParam("id") Long searchId,
            @QueryParam("corpusId") String corpusId,
            @QueryParam("filterLanguage") String filterLanguage,
            @QueryParam("format") String format) throws Exception {
        Search search = Aggregator.getInstance().getSearchById(searchId);
        if (search == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Search job not found").build();
        }
        if (filterLanguage == null || filterLanguage.isEmpty()) {
            filterLanguage = null;
        }

        if (format == null || format.trim().isEmpty() || format.trim().equals("text")) {
            String text = Exports.getExportText(search.getResults(corpusId), filterLanguage);
            return download(text, MediaType.TEXT_PLAIN, search.getQuery() + ".txt");
        } else if (format.equals("tcf")) {
            byte[] bytes = Exports.getExportTCF(
                    search.getResults(corpusId), search.getSearchLanguage(), filterLanguage);
            return download(bytes, TCF_MEDIA_TYPE, search.getQuery() + ".xml");
        } else if (format.equals("ods")) {
            byte[] bytes = Exports.getExportODS(search.getResults(corpusId), filterLanguage);
            return download(bytes, ODS_MEDIA_TYPE, search.getQuery() + ".ods");
        } else if (format.equals("excel")) {
            byte[] bytes = Exports.getExportExcel(search.getResults(corpusId), filterLanguage);
            return download(bytes, EXCEL_MEDIA_TYPE, search.getQuery() + ".xls");
        } else if (format.equals("csv")) {
            String csv = Exports.getExportCSV(search.getResults(corpusId), filterLanguage, ";");
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
    @Path("search/{id}/weblicht-export/{eid}")
    public Response getWeblichtExport(@PathParam("id") Long searchId, @PathParam("eid") Long exportId) {
        Search search = Aggregator.getInstance().getSearchById(searchId);
        if (search == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Search job not found").build();
        }
        byte[] bytes = search.getWeblichtExport(exportId);
        if (bytes == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Weblicht export data not found").build();
        }
        return download(bytes, TCF_MEDIA_TYPE,
                "export-{id}-{eid}.tcf".replace("{id}", searchId.toString()).replace("{eid}", exportId.toString()));
    }

    @GET
    @Path("search/{id}/toWeblicht")
    public Response sendSearchResultsToWeblicht(@PathParam("id") Long searchId,
            @QueryParam("filterLanguage") String filterLanguage,
            @QueryParam("corpusId") String corpusId) throws Exception {
        Search search = Aggregator.getInstance().getSearchById(searchId);
        if (search == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Search job not found").build();
        }
        if (filterLanguage == null || filterLanguage.isEmpty()) {
            filterLanguage = null;
        }

        WeblichtConfig weblicht = Aggregator.getInstance().getParams().getWeblichtConfig();

        String url = null;
        byte[] bytes = Exports.getExportTCF(search.getResults(corpusId), search.getSearchLanguage(), filterLanguage);
        if (bytes != null) {
            // store bytes in-memory TCF weblicht export in searches (searches might only
            // live up to 60min if under high load)
            Long exportId = search.addWeblichtExport(bytes);
            url = "search/{id}/weblicht-export/{eid}".replace("{id}", searchId.toString()).replace("{eid}",
                    exportId.toString());
            url = weblicht.getExportServerUrl() + url;
            log.debug("Export weblicht url: {}", url);
        }

        URI weblichtUri = new URI(weblicht.getUrl() + url);
        return url == null
                ? Response.status(503).entity("error while exporting to weblicht").build()
                : Response.seeOther(weblichtUri).entity(weblichtUri).build();
    }

    @GET
    @Path("statistics")
    public Response getStatistics() throws IOException {
        final Statistics scan = Aggregator.getInstance().getScanStatistics();
        final Statistics search = Aggregator.getInstance().getSearchStatistics();
        final AggregatorConfiguration.Params params = Aggregator.getInstance().getParams();

        Object j = new HashMap<String, Object>() {
            {
                put("Last Scan", new HashMap<String, Object>() {
                    {
                        put("timeout", params.getENDPOINTS_SCAN_TIMEOUT_MS() / 1000.);
                        put("isScan", true);
                        put("institutions", scan.getInstitutions());
                        put("date", scan.getDate());
                    }
                });
                put("Recent Searches", new HashMap<String, Object>() {
                    {
                        put("timeout", params.getENDPOINTS_SEARCH_TIMEOUT_MS() / 1000.);
                        put("isScan", false);
                        put("institutions", search.getInstitutions());
                        put("date", scan.getDate());
                    }
                });
            }
        };
        return Response.ok(toJson(j)).build();
    }

}
