/**
 * @license http://www.gnu.org/licenses/gpl-3.0.txt
 *  GNU General Public License v3
 */
package eu.clarin.sru.fcs.aggregator.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.LoggerFactory;

import eu.clarin.sru.client.fcs.DataViewAdvanced;
import eu.clarin.sru.client.fcs.DataViewHits;
import eu.clarin.sru.fcs.aggregator.scan.Resource;

/**
 * The results of a SRU search-retrieve operation for a particular resource.
 * Its content is json-serialized and sent to the JS client for display.
 *
 * @author Yana Panchenko
 * @author edima
 * @author ljo
 */
public final class Result extends ResultMeta {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Result.class);

    private final Resource resource;

    private List<Kwic> kwics = Collections.synchronizedList(new ArrayList<Kwic>());
    private List<List<AdvancedLayer>> advancedLayers = Collections
            .synchronizedList(new ArrayList<List<AdvancedLayer>>());

    public Result(Resource resource) {
        super(resource);
        this.resource = resource;
    }

    public Resource getResource() {
        return resource;
    }

    public List<Kwic> getKwics() {
        return kwics;
    }

    public List<List<AdvancedLayer>> getAdvancedLayers() {
        return advancedLayers;
    }

    // ----------------------------------------------------------------------

    @Override
    protected void processDataViewHits(DataViewHits dataview, String pid, String reference) {
        final Kwic kwic = new Kwic(dataview, pid, reference);
        kwics.add(kwic);
        log.debug("DataViewHits: {}", kwic.getFragments());
    }

    @Override
    protected void processDataViewAdvanced(DataViewAdvanced dataview, String pid, String reference) {
        List<AdvancedLayer> advLayersSingleGroup = new ArrayList<>();
        for (DataViewAdvanced.Layer layer : dataview.getLayers()) {
            log.debug("DataViewAdvanced layer: {}", dataview.getUnit(), layer.getId());
            final AdvancedLayer aLayer = new AdvancedLayer(layer, pid, reference);
            advLayersSingleGroup.add(aLayer);
        }
        advancedLayers.add(advLayersSingleGroup);
    }

}
