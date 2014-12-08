package eu.clarin.sru.fcs.aggregator.scan;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Allow takes precedence over deny; default is to include
 * everything.
 * Filters for the cache of scan data (endpoint/resources
 * descriptions) based on
 * endpoint url.
 *
 * @author yanapanchenko
 */
public class EndpointUrlFilterDeny implements EndpointFilter {

	private Set<String> deny = new HashSet<String>();

	public EndpointUrlFilterDeny(String... fragments) {
		Collections.addAll(deny, fragments);
	}

	@Override
	public boolean filter(String endpoint) {
		for (String d : deny) {
			if (endpoint.contains(d)) {
				return false;
			}
		}
		return true;
	}

}
