package eu.clarin.sru.fcs.aggregator.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import eu.clarin.sru.client.fcs.DataViewAdvanced;

public class AdvancedLayers {

    private List<AdvancedLayer> layers;
    private String pid;
    private String reference;

    public AdvancedLayers(final DataViewAdvanced dv, String pid, String reference) {
        this.pid = pid;
        this.reference = reference;

        List<AdvancedLayer> aLayers = new ArrayList<>();
        for (DataViewAdvanced.Layer layer : dv.getLayers()) {
            final AdvancedLayer aLayer = new AdvancedLayer(layer);
            aLayers.add(aLayer);
        }
        this.layers = Collections.unmodifiableList(aLayers);
    }

    public List<AdvancedLayer> getLayers() {
        return layers;
    }

    public String getPid() {
        return pid;
    }

    public String getReference() {
        return reference;
    }

}
