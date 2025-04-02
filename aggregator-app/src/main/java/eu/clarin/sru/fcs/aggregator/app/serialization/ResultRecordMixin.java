package eu.clarin.sru.fcs.aggregator.app.serialization;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import eu.clarin.sru.fcs.aggregator.search.AdvancedLayer;
import eu.clarin.sru.fcs.aggregator.search.Kwic;
import eu.clarin.sru.fcs.aggregator.search.LexEntry;

public abstract class ResultRecordMixin {
    @JsonProperty("cql")
    private Kwic kwic;

    @JsonProperty("fcs")
    private List<AdvancedLayer> advancedLayers;

    @JsonProperty("lex")
    private LexEntry lexEntry;
    
    @JsonProperty("lang")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String language;
    
    @JsonProperty("pid")
    private String pid;

    @JsonProperty("ref")
    private String reference;
}
