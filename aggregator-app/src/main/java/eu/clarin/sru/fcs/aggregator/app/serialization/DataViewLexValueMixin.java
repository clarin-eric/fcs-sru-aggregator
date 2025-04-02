package eu.clarin.sru.fcs.aggregator.app.serialization;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class DataViewLexValueMixin {
    @JsonProperty(required = true)
    String value;

    @JsonProperty(value = "id", required = false)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String xmlId;

    @JsonProperty(value = "lang", required = false)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String xmlLang;

    @JsonProperty(required = false)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String langUri;

    @JsonProperty(required = false)
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    boolean preferred;

    @JsonProperty(required = false)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String ref;

    @JsonProperty(required = false)
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    List<String> idRefs;

    @JsonProperty(required = false)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String vocabRef;

    @JsonProperty(required = false)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String vocabValueRef;

    @JsonProperty(required = false)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String type;

    @JsonProperty(required = false)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String source;

    @JsonProperty(required = false)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String sourceRef;

    @JsonProperty(required = false)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String date;
}
