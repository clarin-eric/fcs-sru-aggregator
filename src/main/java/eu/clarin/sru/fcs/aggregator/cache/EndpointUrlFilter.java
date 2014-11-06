package eu.clarin.sru.fcs.aggregator.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Filters for the cache of scan data (endpoint/resources descriptions) based on
 * endpoint url. Only endpoints containing one of the specified string in the
 * endpoint url will be cached. Useful for testing the endpoints.
 *
 * @author yanapanchenko
 */
public class EndpointUrlFilter implements EndpointFilter {

	private Set<String> deny = new HashSet<String>();
	private Set<String> allow = new HashSet<String>();

	public EndpointUrlFilter allow(String... fragments) {
		Collections.addAll(allow, fragments);
		return this;
	}

	public EndpointUrlFilter deny(String... fragments) {
		Collections.addAll(deny, fragments);
		return this;
	}

	@Override
	public Iterable<String> filter(Iterable<String> endpoints) {
		List<String> filtered = new ArrayList<String>();

		for (String endp : endpoints) {
			if (allow.isEmpty()) {
				filtered.add(endp);
			}
			for (String urlSubstring : allow) {
				if (endp.contains(urlSubstring)) {
					filtered.add(endp);
					break;
				}
			}
			for (String urlSubstring : deny) {
				if (endp.contains(urlSubstring)) {
					filtered.remove(endp);
				}
			}
		}

		return filtered;
	}
}
