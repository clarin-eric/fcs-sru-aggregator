package eu.clarin.sru.fcs.aggregator.sresult;

import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRURecord;
import eu.clarin.sru.client.SRUSearchRetrieveRequest;
import eu.clarin.sru.client.SRUSearchRetrieveResponse;
import eu.clarin.sru.client.SRUThreadedClient;
import eu.clarin.sru.client.SRUVersion;
import eu.clarin.sru.client.fcs.ClarinFCSRecordData;
import eu.clarin.sru.client.fcs.ClarinFCSRecordParser;
import eu.clarin.sru.client.fcs.DataViewKWIC;
import eu.clarin.sru.fcs.aggregator.app.WebAppListener;
import eu.clarin.sru.fcs.aggregator.data.Institution;
import eu.clarin.sru.fcs.aggregator.data.SearchResult;
import eu.clarin.sru.fcs.aggregator.sparam.CorpusTreeNodeRenderer;
import eu.clarin.weblicht.wlfxb.io.WLDObjector;
import eu.clarin.weblicht.wlfxb.io.WLFormatException;
import eu.clarin.weblicht.wlfxb.md.xb.MetaData;
import eu.clarin.weblicht.wlfxb.tc.xb.TextCorpusStored;
import eu.clarin.weblicht.wlfxb.xb.WLData;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Menuitem;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Treeitem;

/**
 * Controls search execution: runs requests and displays results inside
 * specified Component
 *
 * @author Yana Panchenko
 */
public class SearchResultsController {

    private List<SearchResult> resultsUnprocessed;
    private List<SearchResult> resultsProcessed;
    private SRUThreadedClient searchClient;
    private Component resultsArea;
    private UpdateResultsThread resultsThread;
    private int currentRequestId = 0;
    private Label progress;
    
    private static final Logger logger = Logger.getLogger(SearchResultsController.class.getName());

    public SearchResultsController(Component resultsArea, Label progress) {
        this.resultsArea = resultsArea;
        this.progress = progress;
        Executions.getCurrent().getDesktop().enableServerPush(true);
        searchClient = (SRUThreadedClient) Executions.getCurrent().getDesktop().getWebApp().getAttribute(WebAppListener.SHARED_SRU_CLIENT);
    }

    public void executeSearch(Set<Treeitem> selectedItems, int maxRecords, String searchString, SRUVersion version) {

        // execute search only if a user selected at least one endpint/corpus
        if (selectedItems.isEmpty()) {
            Messagebox.show("Please select at least one corpus!", "CLARIN-D FCS Aggregator", 0, Messagebox.EXCLAMATION);
            return;
        }
        // execute search only if a user entered a search query
        if (searchString == null || searchString.isEmpty()) {
            Messagebox.show("Please enter a query!", "CLARIN-D FCS Aggregator", 0, Messagebox.EXCLAMATION);
            return;
        }

        // terminate previous search requests and corresponding response processing
        terminateProcessingRequestsAndResponses();

        // update current search request id
        currentRequestId++;

        // clear are where results are to be displayed
        resultsArea.getChildren().clear();

        // empty storage for unprocessed processed lists with recordsData
        resultsProcessed = new ArrayList<SearchResult>();
        resultsUnprocessed = new ArrayList<SearchResult>();

        // finally, send search requests to all the selected by user 
        // endpoints/corpora and process the responses
        sendRequests(selectedItems, maxRecords, searchString, version);
        processResponses();
    }

    private void sendRequests(Set<Treeitem> selectedItems, int maxRecords, String searchString, SRUVersion version) {

        logger.log(Level.INFO, "Executing query={0} maxRecords={1}", 
                new Object[]{searchString, maxRecords});
        
        for (Treeitem selectedItem : selectedItems) {
            Object nodeData = selectedItem.getAttribute(CorpusTreeNodeRenderer.ITEM_DATA);
            if (selectedItem.getParentItem().isSelected() || (nodeData instanceof Institution)) {
                // don't query institution, and don't query subcorpus separately 
                // if there whole parent corpus/endpoint will be queried
            } else {
                SearchResult resultsItem = executeRequest(nodeData, searchString, maxRecords, version);
                resultsUnprocessed.add(resultsItem);
            }
        }
    }

    private SearchResult executeRequest(Object nodeData, String searchString, int maxRecords, SRUVersion version) {

        SearchResult resultsItem = new SearchResult(nodeData);
        logger.log(Level.FINE, "Executing search for {0} query={1} maxRecords={2}", 
                new Object[]{nodeData.toString(), searchString, maxRecords});
        SRUSearchRetrieveRequest searchRequest = new SRUSearchRetrieveRequest(resultsItem.getEndpoint().getUrl());
        searchRequest.setVersion(version);
        searchRequest.setMaximumRecords(maxRecords);
        searchRequest.setRecordSchema(ClarinFCSRecordData.RECORD_SCHEMA);
        searchRequest.setQuery(searchString);
        if (resultsItem.hasCorpusHandler()) {
            searchRequest.setExtraRequestData("x-context", resultsItem.getCorpus().getValue());
        }
        try {
            Future<SRUSearchRetrieveResponse> futureResponse = searchClient.searchRetrieve(searchRequest);
            resultsItem.setFutureResponse(futureResponse);
        } catch (SRUClientException ex) {
            logger.log(Level.SEVERE, "SearchRetrieve failed for {0} {1} {2}", 
                    new String[]{resultsItem.getEndpoint().getUrl(), ex.getClass().getName(), ex.getMessage()});
        }
        return resultsItem;

    }

    private void processResponses() {
        processResponsesWithAsyncResultsWindowUpdate();
        //processResponsesWithSyncResultsWindowUpdate();

    }

//    private void processResponsesWithSyncResultsWindowUpdate() {
//
//        while (!resultsUnprocessed.isEmpty()) {
//            SearchResult resultsItem = resultsUnprocessed.remove(0);
//            if (!resultsItem.isWaitingForResponse()) {
//                resultsItem.consumeResponse();
//                // create groupbox with search results item
//                Groupbox groupbox = createRecordsGroup(resultsItem);
//                // appand this search result only
//                resultsArea.appendChild(groupbox);
//                resultsProcessed.add(resultsItem);
//            } else {
//                resultsUnprocessed.add(resultsItem);
//            }
//
//        }
//    }
    
    private void processResponsesWithAsyncResultsWindowUpdate() {
        resultsThread = new UpdateResultsThread();
        resultsThread.start();
    }

    private class UpdateResultsThread extends Thread {

        @Override
        public void run() {
            while (!resultsUnprocessed.isEmpty() && !Thread.currentThread().isInterrupted()) {
                
                SearchResult resultsItem = resultsUnprocessed.remove(0);
                if (!resultsItem.isWaitingForResponse()) {
                    resultsItem.consumeResponse();
                    Executions.schedule(resultsArea.getDesktop(), new ResponseListener(resultsItem, currentRequestId), new Event("onDummy"));
                    // this alternative to Executions.schedule() - sinchronious update - 
                    // doesn't work: if interrupted here (exception thrown), then
                    // the current thread seems to already get controll of the desktop,
                    // but never gets it back and desktop (page) hangs....
                    ////Obtain the control of UI
                    //Executions.activate(resultsArea.getDesktop());
                    //try {
                    //    updateResultsArea();
                    //} finally {
                    //    //SDeactivate to return the control of UI back
                    //    Executions.deactivate(resultsArea.getDesktop());
                    //}
                    resultsProcessed.add(resultsItem);
                    //System.out.println("RECORDS ITEM ADDED");
                    
                } else {
                    resultsUnprocessed.add(resultsItem);
                }
            }

            if (Thread.currentThread().isInterrupted()) {
                for (SearchResult resultsItem : resultsUnprocessed) {
                    resultsItem.cancelWaitingForResponse();
                }
            }

        }
    }

    private class ResponseListener implements EventListener {

        int requestId;
        SearchResult resultsItem;

        public ResponseListener(SearchResult resultsItem, int requestId) {
            this.resultsItem = resultsItem;
            this.requestId = requestId;
        }

        @Override
        public void onEvent(Event event) {

            // create groupbox with search results item
            Groupbox groupbox = createRecordsGroup(resultsItem);

            // appand this search result only if it
            // is a result of the current request
            if (requestId == currentRequestId) {
                resultsArea.appendChild(groupbox);
            }

            // if in the meanwhile there was a new request
            // this search result is outdated, detach it:
            if (requestId != currentRequestId) {
                groupbox.detach();
            }
            
            if (resultsUnprocessed.isEmpty()) {
                progress.setValue("");
            } else {
                progress.setValue("waiting for " + resultsUnprocessed.size() + " responses...");
            }

        }
    }

    private Groupbox createRecordsGroup(SearchResult resultsItem) {

        Groupbox recordsGroup = new Groupbox();

        // style the box
        recordsGroup.setMold("3d");
        recordsGroup.setSclass("ccsLightBlue");
        recordsGroup.setContentStyle("border:0;");
        recordsGroup.setStyle("margin:10px;10px;10px;10px;");
        recordsGroup.setClosable(true);
        //recordsGroup.setOpen(false);

        // create title
        StringBuilder sb = new StringBuilder();
        sb.append(resultsItem.getEndpoint().getInstitution().getName());
        sb.append(" ");
        sb.append(resultsItem.getEndpoint().getUrl());
        if (resultsItem.hasCorpusHandler()) {
            if (resultsItem.getCorpus().getDisplayTerm() != null) {
                sb.append(" ");
                sb.append(resultsItem.getCorpus().getDisplayTerm());
            }
            if (sb.append(resultsItem.getCorpus().getValue()) != null) {
                sb.append(" ");
                sb.append(resultsItem.getCorpus().getValue());
            }
        }
        recordsGroup.setTitle(sb.toString());

        // populate it with records grid or failure message
        if (resultsItem.getResponse() == null) { // there was an error in response
            recordsGroup.appendChild(new Label("Sorry, the search failed!"));
        } else if (resultsItem.getResponse().hasRecords()) { // the response was fine and there >=1 records
            Grid grid = new Grid();
//            grid.setWidth("100%");
//            grid.setMold("paging");
//            grid.setPageSize(10);
            Columns columns = new Columns();
            Column c;
            c = new Column();
            //c.setLabel("Left");
            columns.appendChild(c);
            //c.setHflex("2");
            c = new Column();
            //c.setLabel("Hit");
            c.setHflex("min");
            //c.setHflex("1");
            columns.appendChild(c);
            c = new Column();
            //c.setHflex("2");
            //c.setLabel("Right");
            columns.appendChild(c);
            grid.appendChild(columns);

            List<SRURecord> sruRecords = resultsItem.getResponse().getRecords();
            ListModel lmodel = new SimpleListModel(sruRecords);
            grid.setModel(lmodel);
            grid.setRowRenderer(new SearchResultRecordRenderer(resultsItem));
            recordsGroup.appendChild(grid);
        } else { // the response was fine, but there are no records
            recordsGroup.appendChild(new Label("Sorry, there were no results!"));
        }

        return recordsGroup;
    }

    public void exportCSV() {

        boolean noResult = true;
        StringBuilder csv = new StringBuilder();
        if (resultsProcessed != null && !resultsProcessed.isEmpty()) {
            for (SearchResult result : resultsProcessed) {
                for (DataViewKWIC kwic : result.getDataKWIC()) {
                    csv.append("\"");
                    csv.append(kwic.getLeft().replace("\"", "QUOTE"));
                    csv.append("\"");
                    csv.append(",");
                    csv.append("\"");
                    csv.append(kwic.getKeyword().replace("\"", "QUOTE"));
                    csv.append("\"");
                    csv.append(",");
                    csv.append("\"");
                    csv.append(kwic.getRight().replace("\"", "QUOTE"));
                    csv.append("\"");
                    csv.append("\n");
                    noResult = false;
                }
            }
        }

        if (noResult) {
            Messagebox.show("Nothing to export!");
        } else {
            Filedownload.save(csv.toString(), "text/plain", "ClarinDFederatedContentSearch.csv");
        }
    }

    public void exportTCF() {

        boolean noResult = true;
        StringBuilder text = new StringBuilder();

        if (resultsProcessed != null && !resultsProcessed.isEmpty()) {
            for (SearchResult result : resultsProcessed) {
                for (DataViewKWIC kwic : result.getDataKWIC()) {
                    text.append(kwic.getLeft());
                    text.append(" ");
                    text.append(kwic.getKeyword());
                    text.append(" ");
                    text.append(kwic.getRight());
                    text.append("\n");
                    noResult = false;
                }
            }

        }

        if (noResult) {
            Messagebox.show("Nothing to export!");
        } else {
            WLData data;
            MetaData md = new MetaData();
            //data.metaData.source = "Tuebingen Uni";
            //md.addMetaDataItem("title", "binding test");
            //md.addMetaDataItem("author", "Yana");
            TextCorpusStored tc = new TextCorpusStored("unknown");
            tc.createTextLayer().addText(text.toString());
            data = new WLData(md, tc);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try {
                WLDObjector.write(data, os);
                Filedownload.save(os.toByteArray(), "text/tcf+xml", "ClarinDFederatedContentSearch.xml");
            } catch (WLFormatException ex) {
                logger.log(Level.SEVERE, "Error exporting TCF {0} {1}", new String[]{ex.getClass().getName(), ex.getMessage()});
                Messagebox.show("Sorry, export error!");
            }
        }
    }

    public void shutdown() {
        terminateProcessingRequestsAndResponses();
    }

    private void terminateProcessingRequestsAndResponses() {

        if (resultsThread != null) {
            resultsThread.interrupt();
            try {
                resultsThread.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(SearchResultsController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        Logger.getLogger(SearchResultsController.class.getName()).log(Level.INFO, "Search terminated");
    }
}