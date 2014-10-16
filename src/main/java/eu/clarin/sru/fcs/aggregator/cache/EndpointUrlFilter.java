package eu.clarin.sru.fcs.aggregator.cache;

import eu.clarin.sru.fcs.aggregator.registry.Endpoint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Filters for the cache of scan data (endpoint/resources descriptions) based on
 * endpoint url. Only endpoints containing one of the specified string in the
 * endpoint url will be cached. Useful for testing the endpoints.
 *
 * @author yanapanchenko
 */
public class EndpointUrlFilter implements EndpointFilter {

	private List<String> allow = new ArrayList<String>();

	public EndpointUrlFilter(String... fragments) {
		Collections.addAll(allow, fragments);
	}

	@Override
	public Iterable<Endpoint> filter(Iterable<Endpoint> endpoints) {
		List<Endpoint> filtered = new ArrayList<Endpoint>();

		for (Endpoint endp : endpoints) {
			for (String urlSubstring : allow) {
				if (endp.getUrl().contains(urlSubstring)) {
					filtered.add(endp);
					break;
				}
			}
		}

		return filtered;
	}

}
