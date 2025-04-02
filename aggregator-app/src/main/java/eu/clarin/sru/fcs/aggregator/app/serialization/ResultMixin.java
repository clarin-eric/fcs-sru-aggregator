package eu.clarin.sru.fcs.aggregator.app.serialization;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import eu.clarin.sru.fcs.aggregator.search.AdvancedLayer;
import eu.clarin.sru.fcs.aggregator.search.Kwic;
import eu.clarin.sru.fcs.aggregator.search.LexEntry;

public abstract class ResultMixin {
    // suppress legacy fields

    @JsonIgnore
    public abstract List<Kwic> getKwics();

    @JsonIgnore
    public abstract List<List<AdvancedLayer>> getAdvancedLayers();

    @JsonIgnore
    public abstract List<LexEntry> getLexEntries();
}
