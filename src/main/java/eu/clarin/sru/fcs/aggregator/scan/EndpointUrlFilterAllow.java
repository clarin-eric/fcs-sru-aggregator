package eu.clarin.sru.fcs.aggregator.scan;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Allow takes precedence over deny; default is to include everything.
 * Filters for the cache of scan data (endpoint/resources descriptions)
 * based on endpoint url.
 *
 * @author yanapanchenko
 */
public class EndpointUrlFilterAllow implements EndpointFilter {

	private Set<String> allow = new HashSet<String>();

	public EndpointUrlFilterAllow(String... fragments) {
		Collections.addAll(allow, fragments);
	}

	@Override
	public boolean filter(String endpoint) {
		for (String a : allow) {
			if (endpoint.contains(a)) {
				return true;
			}
		}
		return false;
	}

}
