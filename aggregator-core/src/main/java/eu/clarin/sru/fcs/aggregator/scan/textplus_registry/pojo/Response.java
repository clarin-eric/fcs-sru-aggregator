package eu.clarin.sru.fcs.aggregator.scan.textplus_registry.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Response<T> {

    int status;
    String action;

    Item<T> item;

    public int getStatus() {
        return status;
    }

    public String getAction() {
        return action;
    }

    public Item<T> getItem() {
        return item;
    }

}
