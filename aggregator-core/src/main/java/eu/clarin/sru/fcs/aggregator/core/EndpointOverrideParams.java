package eu.clarin.sru.fcs.aggregator.core;

import java.util.List;

import eu.clarin.sru.fcs.aggregator.scan.EndpointOverrideConfig;

public interface EndpointOverrideParams {
    List<EndpointOverrideConfig> getEndpointOverrides();
}
