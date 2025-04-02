package eu.clarin.sru.fcs.aggregator.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import eu.clarin.sru.client.SRUDiagnostic;
import eu.clarin.sru.client.SRURecord;
import eu.clarin.sru.client.SRUSearchRetrieveResponse;
import eu.clarin.sru.client.SRUSurrogateRecordData;
import eu.clarin.sru.client.fcs.ClarinFCSRecordData;
import eu.clarin.sru.client.fcs.DataView;
import eu.clarin.sru.client.fcs.DataViewAdvanced;
import eu.clarin.sru.client.fcs.DataViewGenericDOM;
import eu.clarin.sru.client.fcs.DataViewGenericString;
import eu.clarin.sru.client.fcs.DataViewHits;
import eu.clarin.sru.client.fcs.DataViewHitsWithLexAnnotations;
import eu.clarin.sru.client.fcs.DataViewLex;
import eu.clarin.sru.fcs.aggregator.scan.Diagnostic;
import eu.clarin.sru.fcs.aggregator.scan.JavaException;
import eu.clarin.sru.fcs.aggregator.scan.Resource;

public class ResultMeta {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ResultMeta.class);

    protected AtomicReference<String> resourceHandle = new AtomicReference<String>();
    protected AtomicReference<String> endpointUrl = new AtomicReference<String>();

    protected AtomicBoolean inProgress = new AtomicBoolean(true);
    private final CountDownLatch inProgressLatch = new CountDownLatch(1);

    protected AtomicInteger nextRecordPosition = new AtomicInteger(1);
    protected AtomicInteger numberOfRecords = new AtomicInteger(-1);
    protected AtomicInteger numberOfRecordsLoaded = new AtomicInteger(0);

    protected AtomicBoolean hasAdvancedResults = new AtomicBoolean(false);
    protected AtomicBoolean hasLexicalResults = new AtomicBoolean(false);
    protected AtomicBoolean isLexHits = new AtomicBoolean(false);

    protected AtomicReference<JavaException> exception = new AtomicReference<JavaException>();
    protected List<Diagnostic> diagnostics = Collections.synchronizedList(new ArrayList<Diagnostic>());

    public ResultMeta(Resource resource) {
        endpointUrl.set(resource.getEndpoint().getUrl());
        resourceHandle.set(resource.getHandle());
    }

    // ----------------------------------------------------------------------

    public String getResourceHandle() {
        return resourceHandle.get();
    }

    public void setResourceHandle(String handle) {
        resourceHandle.set(handle);
    }

    public String getEndpointUrl() {
        return endpointUrl.get();
    }

    public void setEndpointUrl(String url) {
        endpointUrl.set(url);
    }

    public String getId() {
        return endpointUrl.get() + "#" + resourceHandle.get();
    }

    public void setId(String id) { // dumb setter for JsonDeserialization
    }

    public void setInProgress(boolean inProgress) {
        this.inProgress.set(inProgress);
    }

    public boolean getInProgress() {
        return inProgress.get();
    }

    public List<Diagnostic> getDiagnostics() {
        return Collections.unmodifiableList(diagnostics);
    }

    public JavaException getException() {
        return exception.get();
    }

    public void setException(Exception xc) {
        exception.set(new JavaException(xc));
    }

    public int getNextRecordPosition() {
        return nextRecordPosition.get();
    }

    public int getNumberOfRecords() {
        return numberOfRecords.get();
    }

    public int getNumberOfRecordsLoaded() {
        return numberOfRecordsLoaded.get();
    }

    public boolean hasAdvancedResults() {
        return hasAdvancedResults.get();
    }

    public boolean hasLexicalResults() {
        return hasLexicalResults.get();
    }

    public boolean isLexHits() {
        return isLexHits.get();
    }

    // ----------------------------------------------------------------------

    public void setDone() {
        setInProgress(false);
        inProgressLatch.countDown();
    }

    public void await() throws InterruptedException {
        inProgressLatch.await();
    }

    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return inProgressLatch.await(timeout, unit);
    }

    // ----------------------------------------------------------------------

    public void addResponse(SRUSearchRetrieveResponse response) {
        if (response != null) {
            if (response.hasRecords()) {
                for (SRURecord record : response.getRecords()) {
                    addRecord(record);
                }
            }
            if (response.hasDiagnostics()) {
                for (SRUDiagnostic d : response.getDiagnostics()) {
                    diagnostics.add(new Diagnostic(d.getURI(), d.getMessage(), d.getDetails()));
                }
            }

            if (response.getNextRecordPosition() > 0) {
                nextRecordPosition.set(response.getNextRecordPosition());
            }
            numberOfRecords.set(response.getNumberOfRecords());
        }
    }

    private void addRecord(SRURecord record) {
        nextRecordPosition.incrementAndGet();
        if (record.isRecordSchema(ClarinFCSRecordData.RECORD_SCHEMA)) {
            ClarinFCSRecordData rd = (ClarinFCSRecordData) record.getRecordData();
            eu.clarin.sru.client.fcs.Resource resource = rd.getResource();
            setClarinRecord(resource);
            log.debug("Resource ref={}, pid={}, dataViews={}", resource.getRef(), resource.getPid(),
                    resource.hasDataViews());
        } else if (record.isRecordSchema(SRUSurrogateRecordData.RECORD_SCHEMA)) {
            SRUSurrogateRecordData r = (SRUSurrogateRecordData) record.getRecordData();
            log.info("Surrogate diagnostic: uri={}, message={}, detail={}", r.getURI(), r.getMessage(), r.getDetails());
        } else {
            log.info("Unsupported schema: {}", record.getRecordSchema());
        }
    }

    private void setClarinRecord(eu.clarin.sru.client.fcs.Resource resource) {
        String pid = resource.getPid();
        String reference = resource.getRef();

        if (resource.hasDataViews()) {
            processDataViews(resource.getDataViews(), pid, reference);
        }

        if (resource.hasResourceFragments()) {
            for (eu.clarin.sru.client.fcs.Resource.ResourceFragment fragment : resource.getResourceFragments()) {
                log.debug("ResourceFragment: ref={}, pid={}, dataViews={}", fragment.getRef(), fragment.getPid(),
                        fragment.hasDataViews());
                if (fragment.hasDataViews()) {
                    processDataViews(fragment.getDataViews(),
                            fragment.getPid() != null ? fragment.getPid() : pid,
                            fragment.getRef() != null ? fragment.getRef() : reference);
                }
            }
        }
    }

    private void processDataViews(List<DataView> dataViews, String pid, String reference) {
        for (DataView dataview : dataViews) {
            if (dataview instanceof DataViewGenericDOM) {
                final DataViewGenericDOM view = (DataViewGenericDOM) dataview;
                final Node root = view.getDocument().getFirstChild();
                log.debug("DataView (generic dom): root element <{}> / {}",
                        root.getNodeName(),
                        root.getOwnerDocument().hashCode());
            } else if (dataview instanceof DataViewGenericString) {
                final DataViewGenericString view = (DataViewGenericString) dataview;
                log.debug("DataView (generic string): data = {}",
                        view.getContent());
            } else if (dataview instanceof DataViewHits) {
                numberOfRecordsLoaded.incrementAndGet();

                // detection if Lex Hits Data View
                if (dataview instanceof DataViewHitsWithLexAnnotations) {
                    final Kwic kwic = new Kwic((DataViewHits) dataview, pid, reference);
                    // if we find any hit with a hitKind with "lex-" prefix
                    // then we probably have a LexHits Data View
                    kwic.getFragments().stream().filter(f -> f.isHit())
                            .filter(f -> Optional.ofNullable(f.getHitKind()).orElse("").toLowerCase()
                                    .startsWith("lex-"))
                            .findAny().ifPresent((k) -> isLexHits.set(true));
                }

                processDataViewHits((DataViewHits) dataview, pid, reference);
            } else if (dataview instanceof DataViewAdvanced) {
                hasAdvancedResults.set(true);
                processDataViewAdvanced((DataViewAdvanced) dataview, pid, reference);
            } else if (dataview instanceof DataViewLex) {
                hasLexicalResults.set(true);
                processDataViewLex((DataViewLex) dataview, pid, reference);
            }
        }
    }

    // for overriding

    protected void processDataViewHits(final DataViewHits dataview, final String pid, final String reference) {
        // empty
    }

    protected void processDataViewAdvanced(final DataViewAdvanced dataview, final String pid, final String reference) {
        // empty
    }

    protected void processDataViewLex(final DataViewLex dataview, final String pid, final String reference) {
        // empty
    }

}
