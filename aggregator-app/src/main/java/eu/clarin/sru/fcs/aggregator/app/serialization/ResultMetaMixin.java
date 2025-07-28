package eu.clarin.sru.fcs.aggregator.app.serialization;

import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class ResultMetaMixin {
    @JsonProperty("hasAdvResults")
    abstract boolean hasAdvancedResults();

    @JsonProperty("hasLexResults")
    abstract boolean hasLexicalResults();

    @JsonProperty("isLexHits")
    abstract boolean isLexHits();
}
