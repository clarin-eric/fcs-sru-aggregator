package eu.clarin.sru.fcs.aggregator.rest;

import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;

import eu.clarin.sru.fcs.aggregator.search.MetaOnlyResult;

public class JsonMetaOnlySearch {
    @JsonProperty(required = true)
    int inProgress = 0;

    @JsonProperty(required = true)
    List<MetaOnlyResult> results;

    public JsonMetaOnlySearch(List<MetaOnlyResult> results) {
        this.results = results;
    }

    public static JsonMetaOnlySearch fromJsonSearch(JsonSearch search) {
        final List<MetaOnlyResult> results = search.results.stream().map(r -> new MetaOnlyResult(r))
                .collect(Collectors.toList());
        final JsonMetaOnlySearch js = new JsonMetaOnlySearch(results);
        js.inProgress = search.inProgress;
        return js;
    }
}
