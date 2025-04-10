package eu.clarin.sru.fcs.aggregator.scan.centre_registry.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Consortium {

    // https://centres.clarin.eu/api/model/Consortium

    @JsonProperty("pk")
    int id;

    ConsortiumFields fields;

    public int getId() {
        return id;
    }

    public ConsortiumFields getFields() {
        return fields;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConsortiumFields {
        @JsonProperty("country_code")
        String countryCode;

        @JsonProperty("country_name")
        String countryName;

        @JsonProperty("is_observer")
        boolean isObserver;

        String name;

        @JsonProperty("website_url")
        String websiteUrl;

        String alias;

        public String getCountryCode() {
            return countryCode;
        }

        public String getCountryName() {
            return countryName;
        }

        public boolean isObserver() {
            return isObserver;
        }

        public String getName() {
            return name;
        }

        public String getWebsiteUrl() {
            return websiteUrl;
        }

        public String getAlias() {
            return alias;
        }

    }
}
