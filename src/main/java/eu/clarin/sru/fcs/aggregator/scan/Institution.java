package eu.clarin.sru.fcs.aggregator.scan;

import java.util.*;

/**
 * Institution. Contains information about institution name and link (url). Can
 * have information about its CQL Endpoints.
 *
 * @author Yana Panchenko
 */
public class Institution {

	private String name;
	private String link;
	private Set<String> endpoints;

	// for JSON deserialization
	public Institution() {
	}

	public Institution(String name, String link) {
		this.name = name;
		this.link = link;
		this.endpoints = new LinkedHashSet<String>();
	}

	public String addEndpoint(String endpointUrl) {
		endpoints.add(endpointUrl);
		return endpointUrl;
	}

	public String getName() {
		return name;
	}

	public String getLink() {
		return link;
	}

	public Set<String> getEndpoints() {
		return this.endpoints;
	}

	@Override
	public String toString() {
		if (name != null && name.length() > 0) {
			return name;
		} else {
			return link;
		}
	}

	@Override
	public int hashCode() {
		https://primes.utm.edu/lists/small/1000.txt
		return name.hashCode() * 2953;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Institution)) {
			return false;
		}
		Institution i = (Institution) obj;
		return i.name.equals(name);
	}
}
