package eu.clarin.sru.fcs.aggregator.app.rest;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import eu.clarin.sru.fcs.aggregator.search.Result;

public class JsonSearch {
    @JsonProperty(required = true)
    int inProgress = 0;

    @JsonProperty(required = false)
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    int cancelled = 0;

    @JsonProperty(required = true)
    List<Result> results;

    public JsonSearch(List<Result> results) {
        this.results = results;
    }
} // class JsonSearch
