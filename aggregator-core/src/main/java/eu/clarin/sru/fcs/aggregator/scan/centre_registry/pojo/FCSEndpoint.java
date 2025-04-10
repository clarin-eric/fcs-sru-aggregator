package eu.clarin.sru.fcs.aggregator.scan.centre_registry.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FCSEndpoint {

    // https://centres.clarin.eu/api/model/FCSEndpoint

    FCSEndpointFields fields;

    public FCSEndpointFields getFields() {
        return fields;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FCSEndpointFields {
        @JsonProperty("centre")
        int centreId;

        String uri;

        String note;

        public int getCentreId() {
            return centreId;
        }

        public String getUri() {
            return uri;
        }

        public String getNote() {
            return note;
        }

    }
}
