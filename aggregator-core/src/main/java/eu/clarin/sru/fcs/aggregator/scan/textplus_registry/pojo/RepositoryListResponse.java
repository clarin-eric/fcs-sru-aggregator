package eu.clarin.sru.fcs.aggregator.scan.textplus_registry.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RepositoryListResponse extends ListResponse<Repository> {
}
