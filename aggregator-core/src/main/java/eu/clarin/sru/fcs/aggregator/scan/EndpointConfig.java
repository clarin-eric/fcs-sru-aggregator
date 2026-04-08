package eu.clarin.sru.fcs.aggregator.scan;

import java.net.URL;

public interface EndpointConfig {
    URL getUrl();

    String getName();

    String getWebsite();
}
