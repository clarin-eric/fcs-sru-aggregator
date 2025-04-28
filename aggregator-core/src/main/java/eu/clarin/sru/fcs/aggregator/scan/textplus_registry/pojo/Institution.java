package eu.clarin.sru.fcs.aggregator.scan.textplus_registry.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Institution {

    // // https://registry.text-plus.org/api/v1/e/institution/

    String info;
    String name;

    public String getInfo() {
        return info;
    }

    public String getName() {
        return name;
    }

}
