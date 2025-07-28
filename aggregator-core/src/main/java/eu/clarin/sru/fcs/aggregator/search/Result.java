/**
 * @license http://www.gnu.org/licenses/gpl-3.0.txt
 *  GNU General Public License v3
 */
package eu.clarin.sru.fcs.aggregator.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import eu.clarin.sru.client.SRURecord;
import eu.clarin.sru.client.fcs.DataViewAdvanced;
import eu.clarin.sru.client.fcs.DataViewHits;
import eu.clarin.sru.client.fcs.DataViewLex;
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
    private final PerformLanguageDetectionCallback langDetectCallback;

    public List<Record> records = Collections.synchronizedList(new ArrayList<>());

    public Result(Resource resource, PerformLanguageDetectionCallback langDetectCallback) {
        super(resource);
        this.resource = resource;
        this.langDetectCallback = langDetectCallback;
    }

    public Resource getResource() {
        return resource;
    }

    // ----------------------------------------------------------------------

    public List<Record> getRecords() {
        return records;
    }

    public List<Kwic> getKwics() {
        return records.stream().map(r -> (r instanceof ResultRecord) ? ((ResultRecord) r).getKwic() : null)
                .filter(r -> r != null).collect(Collectors.toList());
    }

    public List<List<AdvancedLayer>> getAdvancedLayers() {
        return records.stream()
                .map(r -> (r instanceof ResultRecord) ? ((ResultRecord) r).getAdvancedLayers().getLayers() : null)
                .filter(r -> r != null).collect(Collectors.toList());
    }

    public List<LexEntry> getLexEntries() {
        return records.stream().map(r -> (r instanceof ResultRecord) ? ((ResultRecord) r).getLexEntry() : null)
                .filter(r -> r != null).collect(Collectors.toList());
    }

    // ----------------------------------------------------------------------

    @Override
    protected Record addRecord(final SRURecord record) {
        // use default processing but let's store the record
        // we populate with the processDataView* methods below
        final Record resultRecord = super.addRecord(record);
        records.add(resultRecord);
        return resultRecord;
    }

    @Override
    protected void processDataViewHits(final ResultRecord record, final DataViewHits dataview, String pid,
            String reference) {
        final Kwic kwic = new Kwic(dataview);

        // auto-detect language for KWIC
        if (langDetectCallback != null) {
            String dvText = dataview.getText();
            String language = langDetectCallback.detect(dvText);
            record.setLanguage(language);
        }

        record.setKwic(kwic);
        log.debug("DataViewHits: #fragments={}", kwic.getFragments());
    }

    @Override
    protected void processDataViewAdvanced(final ResultRecord record, final DataViewAdvanced dataview, String pid,
            String reference) {
        final AdvancedLayers layers = new AdvancedLayers(dataview, pid, reference);
        record.setAdvancedLayers(layers);
        for (DataViewAdvanced.Layer layer : dataview.getLayers()) {
            log.debug("DataViewAdvanced layer: unit='{}' id='{}'", dataview.getUnit(), layer.getId());
        }
    }

    @Override
    protected void processDataViewLex(final ResultRecord record, final DataViewLex dataview, String pid,
            String reference) {
        final LexEntry entry = new LexEntry(dataview, pid, reference);
        record.setLexEntry(entry);
        log.debug("DataViewLex: #fields={}", entry.getFields().size());
    }

}
