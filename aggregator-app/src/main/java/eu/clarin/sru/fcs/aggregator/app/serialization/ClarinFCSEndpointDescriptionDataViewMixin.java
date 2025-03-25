package eu.clarin.sru.fcs.aggregator.app.serialization;

import com.fasterxml.jackson.annotation.JsonProperty;

import eu.clarin.sru.client.fcs.ClarinFCSEndpointDescription;

public abstract class ClarinFCSEndpointDescriptionDataViewMixin {
    ClarinFCSEndpointDescriptionDataViewMixin(@JsonProperty("identifier") String identifier,
            @JsonProperty("mimeType") String mimeType,
            @JsonProperty("deliveryPolicy") ClarinFCSEndpointDescription.DataView.DeliveryPolicy deliveryPolicy) {
    }
}
