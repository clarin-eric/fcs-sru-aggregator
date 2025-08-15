package eu.clarin.sru.fcs.aggregator.app.serialization;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class InstitutionMixin {
    @JsonProperty(required = false)
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    boolean sideloaded;

    @JsonProperty(required = false)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    String consortium;
}
