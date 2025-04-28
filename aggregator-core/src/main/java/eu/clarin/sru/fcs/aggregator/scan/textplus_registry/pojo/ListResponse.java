package eu.clarin.sru.fcs.aggregator.scan.textplus_registry.pojo;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ListResponse<T> {

    int status;
    String action;
    int size;

    List<Item<T>> items;

    @JsonProperty("_links")
    Map<String, String> links;

    public int getStatus() {
        return status;
    }

    public String getAction() {
        return action;
    }

    public int getSize() {
        return size;
    }

    public List<Item<T>> getItems() {
        return items;
    }

    public Map<String, String> getLinks() {
        return links;
    }

}
