package eu.clarin.sru.fcs.aggregator.scan;

/**
 * @author yanapanchenko
 * @author edima
 * @author ekoerner
 */
public interface EndpointFilter {

    /**
     * Check if endpoint (url) should be included or not.
     * 
     * @param endpoint the endpoint url
     * @return true if the endpoint should be included, false otherwise
     */
    default boolean filter(String endpoint) {
        return true;
    }

    /**
     * Check if endpoint should be included or not based on consortium.
     * 
     * @param consortium consortium name or <code>null</code> if not set
     * @return true if the endpoint should be included, false otherwise
     */
    default boolean filterConsortium(String consortium) {
        return true;
    }
}
