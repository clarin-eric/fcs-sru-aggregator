package eu.clarin.sru.fcs.aggregator.app.serialization;

import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class ResultMetaMixin {
    @JsonProperty("hasAdvResults")
    public abstract boolean hasAdvancedResults();

    @JsonProperty("hasLexResults")
    public abstract boolean hasLexicalResults();

    @JsonProperty("isLexHits")
    public abstract boolean isLexHits();
}
