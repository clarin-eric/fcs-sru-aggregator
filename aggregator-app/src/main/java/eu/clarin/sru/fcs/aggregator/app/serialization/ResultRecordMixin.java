package eu.clarin.sru.fcs.aggregator.app.serialization;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import eu.clarin.sru.fcs.aggregator.search.AdvancedLayers;
import eu.clarin.sru.fcs.aggregator.search.Kwic;
import eu.clarin.sru.fcs.aggregator.search.LexEntry;

public abstract class ResultRecordMixin {
    @JsonProperty("hits")
    Kwic kwic;

    @JsonProperty("adv")
    AdvancedLayers advancedLayers;

    @JsonProperty("lex")
    LexEntry lexEntry;

    @JsonProperty("lang")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String language;

    @JsonProperty("pid")
    String pid;

    @JsonProperty("ref")
    String reference;
}
