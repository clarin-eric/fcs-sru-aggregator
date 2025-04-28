package eu.clarin.sru.fcs.aggregator.scan.textplus_registry.pojo;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Repository {

    // https://registry.text-plus.org/api/v1/e/repository/

    @JsonProperty("primary_name")
    String primaryName;

    List<InstitutionRelation> institution;

    @JsonProperty("uri_iri_fcs")
    List<String> uriIriFcs;

    @JsonProperty("uri_iri_repository")
    String uriIriRepository;

    public List<String> getUriIriFcs() {
        return uriIriFcs;
    }

    public String getUriIriRepository() {
        return uriIriRepository;
    }

    public List<InstitutionRelation> getInstitution() {
        return institution;
    }

    public String getPrimaryName() {
        return primaryName;
    }

    public static class InstitutionRelation {

        Institution institution;

        @JsonProperty("relation_type")
        List<RelationType> relationType;

        public Institution getInstitution() {
            return institution;
        }

        public List<RelationType> getRelationType() {
            return relationType;
        }

        public static class Institution {

            @JsonProperty("@reference")
            String reference;

            boolean entryReference;
            String entity;

            @JsonProperty("_links")
            Map<String, String> links;

            public String getReference() {
                return reference;
            }

            public boolean isEntryReference() {
                return entryReference;
            }

            public String getEntity() {
                return entity;
            }

            public Map<String, String> getLinks() {
                return links;
            }

        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class RelationType {

            @JsonProperty("@reference")
            String reference;

            boolean entryReference;
            String vocabulary;

            @JsonProperty("_links")
            String links;

            public String getReference() {
                return reference;
            }

            public boolean isEntryReference() {
                return entryReference;
            }

            public String getVocabulary() {
                return vocabulary;
            }

            public String getLinks() {
                return links;
            }

        }

    }

}
