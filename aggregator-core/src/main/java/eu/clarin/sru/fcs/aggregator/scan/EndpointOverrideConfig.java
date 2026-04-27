package eu.clarin.sru.fcs.aggregator.scan;

public interface EndpointOverrideConfig extends EndpointConfig {
    /**
     * Whether this endpoint override is active or not. If inactive ({@code false})
     * everything will be ignored.
     * 
     * @return {@code true} if the endpoint override is enabled and should be used
     */
    boolean isEnabled();

    /**
     * Endpoint configuration is to be considered a configuration override if
     * {@code true}.
     * If {@code false}, a new endpoint will be added if the endpoint URL does not
     * yet exist.
     * 
     * @return {@code true} if the endpoint override is <strong>only</strong> an
     *         override and will not be used if there is not endpoint with this URL.
     */
    boolean isOverrideOnly();

    int getMaxConcurrentSearchRequests();

    boolean isCQL();
}
