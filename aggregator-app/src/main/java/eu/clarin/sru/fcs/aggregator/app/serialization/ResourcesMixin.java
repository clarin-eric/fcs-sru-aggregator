package eu.clarin.sru.fcs.aggregator.app.serialization;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import eu.clarin.sru.fcs.aggregator.scan.Institution;
import eu.clarin.sru.fcs.aggregator.scan.Resource;

public abstract class ResourcesMixin {
    @JsonProperty
    private List<Institution> institutions;
    @JsonProperty
    private List<Resource> resources;
}
