package eu.clarin.sru.fcs.aggregator.scan;

public interface EndpointOverrideConfig extends EndpointConfig {
    boolean isEnabled();

    int getMaxConcurrentSearchRequests();

    boolean isCQL();
}
