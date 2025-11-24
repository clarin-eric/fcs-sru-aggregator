package eu.clarin.sru.fcs.aggregator.app.rest;

import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize.Typing;

import eu.clarin.sru.fcs.aggregator.search.ResultMeta;

public class JsonMetaOnlySearch {
    @JsonProperty(required = true)
    int inProgress = 0;

    @JsonProperty(required = false)
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    int cancelled = 0;

    @JsonProperty(required = true)
    @JsonSerialize(typing = Typing.STATIC)
    List<ResultMeta> results;

    public JsonMetaOnlySearch(List<ResultMeta> results) {
        this.results = results;
    }

    public static JsonMetaOnlySearch fromJsonSearch(JsonSearch search) {
        final List<ResultMeta> results = search.results.stream().map(r -> (ResultMeta) r)
                .collect(Collectors.toList());
        final JsonMetaOnlySearch js = new JsonMetaOnlySearch(results);
        js.inProgress = search.inProgress;
        js.cancelled = search.cancelled;
        return js;
    }
} // class JsonMetaOnlySearch
