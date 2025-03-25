package eu.clarin.sru.fcs.aggregator.app.serialization;

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonProperty;

import eu.clarin.sru.client.fcs.ClarinFCSEndpointDescription;

public abstract class ClarinFCSEndpointDescriptionLayerMixin {
    ClarinFCSEndpointDescriptionLayerMixin(@JsonProperty("identifier") String identifier,
            @JsonProperty("resultId") URI resultId, @JsonProperty("layerType") String layerType,
            @JsonProperty("encoding") ClarinFCSEndpointDescription.Layer.ContentEncoding encoding,
            @JsonProperty("qualifier") String qualifier, @JsonProperty("altValueInfo") String altValueInfo,
            @JsonProperty("altValueInfoURI") URI altValueInfoURI) {
    }
}
