package eu.clarin.sru.fcs.aggregator.scan;

/**
 * @author yanapanchenko
 * @author edima
 */
public interface EndpointFilter {

	/**
	 * @return true if the endpoint should be included, false otherwise
	 */
	boolean filter(String endpoint);
}
