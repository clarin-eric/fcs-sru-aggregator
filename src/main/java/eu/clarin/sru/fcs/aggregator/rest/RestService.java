package eu.clarin.sru.fcs.aggregator.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import eu.clarin.sru.client.SRUVersion;
import eu.clarin.sru.fcs.aggregator.app.Aggregator;
import eu.clarin.sru.fcs.aggregator.app.AggregatorConfiguration;
import eu.clarin.sru.fcs.aggregator.client.ThrottledClient;
import eu.clarin.sru.fcs.aggregator.scan.Corpus;
import eu.clarin.sru.fcs.aggregator.scan.Diagnostic;
import eu.clarin.sru.fcs.aggregator.scan.Statistics;
import eu.clarin.sru.fcs.aggregator.search.Request;
import eu.clarin.sru.fcs.aggregator.search.Result;
import eu.clarin.sru.fcs.aggregator.search.Search;
import eu.clarin.sru.fcs.aggregator.lang.LanguagesISO693_3;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.LoggerFactory;

/**
 *
 * @author edima
 */
@Produces(MediaType.APPLICATION_JSON)
@Path("/")
public class RestService {

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(RestService.class);

	ObjectWriter ow = new ObjectMapper().writerWithDefaultPrettyPrinter();

	@Context
	HttpServletRequest request;
	@Context
	ServletContext servletContext;

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
	@Path("statistics")
	public Response getStatistics() throws IOException {
		final Statistics scan = Aggregator.getInstance().getScanStatistics();
		final Statistics search = Aggregator.getInstance().getSearchStatistics();
		final AggregatorConfiguration.Params params = Aggregator.getInstance().getParams();

		Object j = new HashMap<String, Object>() {
			{
				put("lastScanStats", new HashMap<String, Object>() {
					{
						put("maxConcurrentScanRequestsPerEndpoint",
								Aggregator.getInstance().getParams().getSCAN_MAX_CONCURRENT_REQUESTS_PER_ENDPOINT());
						put("timeout", params.getENDPOINTS_SCAN_TIMEOUT_MS() / 1000.);
						put("institutions", scan.getInstitutions());
					}
				});
				put("searchStats", new HashMap<String, Object>() {
					{
						put("maxConcurrentSearchRequestsPerEndpoint",
								Aggregator.getInstance().getParams().getSEARCH_MAX_CONCURRENT_REQUESTS_PER_ENDPOINT());
						put("timeout", params.getENDPOINTS_SEARCH_TIMEOUT_MS() / 1000.);
						put("institutions", search.getInstitutions());
					}
				});
			}
		};
		return Response.ok(toJson(j)).build();
	}

	@GET
	@Path("languages")
	public Response getLanguages() throws IOException {
		Map<String, String> languages = new HashMap<String, String>();
		Set<String> codes = Aggregator.getInstance().getCorpora().getLanguages();
		log.info("get language codes", codes);
		for (String code : codes) {
			String name = LanguagesISO693_3.getInstance().nameForCode(code);
			languages.put(code, name != null ? name : code);
		}
		return Response.ok(toJson(languages)).build();
	}

	@GET
	@Path("diagnostics")
	public Response getDiagnostics() throws IOException {
		Map<String, Diagnostic> diagnostics = Aggregator.getInstance().getCorpora().getEndpointDiagnostics();
		return Response.ok(toJson(diagnostics)).build();
	}

	@POST
	@Path("search")
	public Response postSearch(
			@FormParam("query") String query,
			@FormParam("numberOfResults") Integer numberOfResults,
			@FormParam("language") String language,
			@FormParam("corporaIds[]") List<String> corporaIds) throws Exception {
		if (query == null || query.isEmpty()) {
			return Response.status(400).entity("'query' parameter expected").build();
		}
//		log.info("POST /search corporaIds: " + corporaIds);
		if (corporaIds == null || corporaIds.isEmpty()) {
			return Response.status(400).entity("'corporaIds' parameter expected").build();
		}
		List<Corpus> corpora = Aggregator.getInstance().getCorpora().getCorporaByIds(new HashSet<String>(corporaIds));
		if (corpora == null || corpora.isEmpty()) {
			return Response.status(503).entity("No corpora, please wait for the server to finish scanning").build();
		}
		if (numberOfResults == null || numberOfResults < 10) {
			numberOfResults = 10;
		}
		if (numberOfResults > 250) {
			numberOfResults = 250;
		}
		for (String c : corporaIds) {

		}
		Search search = Aggregator.getInstance().startSearch(SRUVersion.VERSION_1_2, corpora, query, language, numberOfResults);
		if (search == null) {
			return Response.status(500).entity("Initiating search failed").build();
		}
		URI uri = URI.create("" + search.getId());
		return Response.created(uri).entity(uri).build();
	}

	public static class JsonSearch {

		List<Request> requests;
		List<Result> results;

		public JsonSearch(List<Request> requests, List<Result> results) {
			this.requests = requests;
			this.results = results;
		}

		public List<Request> getRequests() {
			return requests;
		}

		public List<Result> getResults() {
			return results;
		}
	}

	@GET
	@Path("search/{id}")
	public Response getSearch(@PathParam("id") Long searchId) throws Exception {
		Search search = Aggregator.getInstance().getSearchById(searchId);
		if (search == null) {
			return Response.status(Response.Status.NOT_FOUND).entity("Search job not found").build();
		}
//		for (Request r : search.getRequests()) {
//			System.out.println("request: " + toJson(r));
//		}
//		for (Result r : search.getResults()) {
//			System.out.println("result: ");
//			System.out.println("    kwics: " + toJson(r.getKwics()));
//			System.out.println("    exc: " + r.getException());
//		}
		JsonSearch js = new JsonSearch(search.getRequests(), search.getResults());
		return Response.ok(js).build();
	}
}
