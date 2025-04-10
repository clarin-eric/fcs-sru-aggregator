package eu.clarin.sru.fcs.aggregator.scan.centre_registry.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Centre {

    // https://centres.clarin.eu/api/model/Centre

    @JsonProperty("pk")
    int id;

    CentreFields fields;

    public int getId() {
        return id;
    }

    public CentreFields getFields() {
        return fields;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CentreFields {
        String name;

        String shorthand;

        String institution;

        @JsonProperty("website_url")
        String websiteUrl;

        @JsonProperty("consortium")
        int consortiumId;

        public String getName() {
            return name;
        }

        public String getShorthand() {
            return shorthand;
        }

        public String getInstitution() {
            return institution;
        }

        public String getWebsiteUrl() {
            return websiteUrl;
        }

        public int getConsortiumId() {
            return consortiumId;
        }

    }

}
