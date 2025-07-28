package eu.clarin.sru.fcs.aggregator.app.serialization;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class LexEntryMixin {
    @JsonIgnore
    abstract String getPid();

    @JsonIgnore
    abstract String getReference();
}
