package eu.clarin.sru.fcs.aggregator.app.serialization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "id", "type" })
public abstract class ClarinFCSEndpointDescriptionLexFieldMixin {

    ClarinFCSEndpointDescriptionLexFieldMixin(@JsonProperty("id") String identifier,
            @JsonProperty("type") String fieldType) {
    }

    @JsonProperty(value = "id", required = true)
    String identifier;

    @JsonProperty(value = "type", required = true)
    String fieldType;
}
