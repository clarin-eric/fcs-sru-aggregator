package eu.clarin.sru.fcs.aggregator.app.serialization;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class ResultMetaMixin {
    @JsonProperty("hasAdvResults")
    abstract boolean hasAdvancedResults();

    @JsonProperty("hasLexResults")
    abstract boolean hasLexicalResults();

    @JsonProperty("isLexHits")
    abstract boolean isLexHits();

    // skip fields that are not filled

    @JsonProperty(required = false)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    abstract String getRequestUrl();

} // abstract class ResultMetaMixin
