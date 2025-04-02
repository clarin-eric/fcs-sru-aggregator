package eu.clarin.sru.fcs.aggregator.app.serialization;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import eu.clarin.sru.client.fcs.DataViewLex;

public abstract class DataViewLexFieldMixin {
    @JsonProperty(required = true)
    DataViewLex.FieldType type;

    @JsonProperty(required = true)
    List<DataViewLex.Value> values;
}
