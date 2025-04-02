package eu.clarin.sru.fcs.aggregator.app.serialization;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import eu.clarin.sru.client.fcs.DataViewLex;

public abstract class DataViewLexMixin {
    @JsonProperty(required = true)
    List<DataViewLex.Field> fields;

    @JsonProperty(value = "lang", required = false)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String xmlLang;

    @JsonProperty(required = false)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String langUri;
}
