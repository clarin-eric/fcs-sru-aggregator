package eu.clarin.sru.fcs.aggregator.app.serialization;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import eu.clarin.sru.fcs.aggregator.search.AdvancedLayer;
import eu.clarin.sru.fcs.aggregator.search.Kwic;
import eu.clarin.sru.fcs.aggregator.search.LexEntry;

public abstract class ResultMixin {
    // suppress legacy fields

    @JsonIgnore
    abstract List<Kwic> getKwics();

    @JsonIgnore
    abstract List<List<AdvancedLayer>> getAdvancedLayers();

    @JsonIgnore
    abstract List<LexEntry> getLexEntries();
}
