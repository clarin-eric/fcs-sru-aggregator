package eu.clarin.sru.fcs.aggregator.app.serialization;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class ClarinFCSEndpointDescriptionExampleQueryMixin {
    ClarinFCSEndpointDescriptionExampleQueryMixin(@JsonProperty("query") String query,
            @JsonProperty("queryType") String queryType, @JsonProperty("description") Map<String, String> description) {
    }
}
