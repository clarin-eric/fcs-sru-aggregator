package eu.clarin.sru.fcs.aggregator.app.serialization;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonValue;

import eu.clarin.sru.fcs.aggregator.search.AdvancedLayer;

public abstract class AdvancedLayersMixin {
    @JsonValue
    abstract List<AdvancedLayer> getLayers();
}
