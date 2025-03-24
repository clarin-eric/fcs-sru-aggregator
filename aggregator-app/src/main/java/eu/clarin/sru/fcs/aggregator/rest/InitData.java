package eu.clarin.sru.fcs.aggregator.rest;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import eu.clarin.sru.fcs.aggregator.scan.Resource;

public class InitData {
    @JsonProperty(required = true)
    List<Resource> resources;

    @JsonProperty(required = true)
    Map<String, String> languages;

    @JsonProperty(required = true)
    List<String> weblichtLanguages;

    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String query;

    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String mode;

    @JsonProperty("x-aggregation-context")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Map<String, List<String>> aggregationContext;
}
