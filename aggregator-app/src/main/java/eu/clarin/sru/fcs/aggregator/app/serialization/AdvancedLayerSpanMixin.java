package eu.clarin.sru.fcs.aggregator.app.serialization;

import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class AdvancedLayerSpanMixin {
    @JsonProperty
    public abstract long[] getRange();
}
