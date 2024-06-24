package eu.clarin.sru.fcs.aggregator.rest;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import eu.clarin.sru.fcs.aggregator.scan.Resource;

public class InitSchema {
    @JsonProperty(required = true)
    List<Resource> resources;

    @JsonProperty(required = true)
    List<String> languages;

    @JsonProperty(required = true)
    List<String> weblichtLanguages;

    @JsonProperty
    String query;

    @JsonProperty
    String mode;

    @JsonProperty("x-aggregation-context")
    Map<String, List<String>> contextString;
}
