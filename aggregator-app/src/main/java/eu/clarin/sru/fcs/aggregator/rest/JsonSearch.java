package eu.clarin.sru.fcs.aggregator.rest;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import eu.clarin.sru.fcs.aggregator.search.Result;

public class JsonSearch {
    @JsonProperty(required = true)
    int inProgress = 0;

    @JsonProperty(required = true)
    List<Result> results;

    public JsonSearch(List<Result> results) {
        this.results = results;
    }
}
