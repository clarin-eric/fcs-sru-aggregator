package eu.clarin.sru.fcs.aggregator.rest;

import eu.clarin.sru.client.SRUVersion;
import eu.clarin.sru.fcs.aggregator.app.Aggregator;
import eu.clarin.sru.fcs.aggregator.registry.Corpus;
import eu.clarin.sru.fcs.aggregator.registry.Languages;
import eu.clarin.sru.fcs.aggregator.search.Request;
import eu.clarin.sru.fcs.aggregator.search.Result;
import eu.clarin.sru.fcs.aggregator.search.Search;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @author edima
 */
@Produces(MediaType.APPLICATION_JSON)
@Path("/")
public class RestService {

	@Context
	HttpServletRequest request;
	@Context
	ServletContext servletContext;

	@GET
	@Path("corpora")
	public Response getCorpora() throws IOException {
		List<Corpus> corpora = Aggregator.getInstance().getScanCache().getRootCorpora();
		return Response.ok(corpora).build();
	}

	public static class JsonLang {

		String name, code;

		public JsonLang(String name, String code) {
			this.name = name;
			this.code = code;
		}

		public String getName() {
			return name;
		}

		public String getCode() {
			return code;
		}
	}

	@GET
	@Path("languages")
	public Response getLanguages() throws IOException {
		Set<String> codes = Aggregator.getInstance().getScanCache().getLanguages();
		List<JsonLang> languages = new ArrayList<JsonLang>();
		for (String code : codes) {
			languages.add(new JsonLang(Languages.getInstance().nameForCode(code), code));
		}
		Collections.sort(languages, new Comparator<JsonLang>() {
			@Override
			public int compare(JsonLang l1, JsonLang l2) {
				return l1.name.compareToIgnoreCase(l2.name);
			}
		});
		return Response.ok(languages).build();
	}

	@POST
	@Path("search")
	public Response postSearch(@FormParam("query") String query) throws Exception {
		if (query == null || query.isEmpty()) {
			return Response.status(400).entity("'query' parameter expected").build();
		}
		List<Corpus> corpora = Aggregator.getInstance().getScanCache().getRootCorpora();
		Search search = Aggregator.getInstance().startSearch(SRUVersion.VERSION_1_2, corpora, query, "eng", 10);
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
		JsonSearch js = new JsonSearch(search.getRequests(), search.getResults());
		return Response.ok(js).build();
	}
}
