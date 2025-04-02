package eu.clarin.sru.fcs.aggregator.app.serialization;

import com.fasterxml.jackson.annotation.JsonValue;

public abstract class DataViewLexFieldTypeMixin {
    @JsonValue
    abstract String getType();
}
