package eu.clarin.sru.fcs.aggregator.app;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.jersey.api.client.Client;
import eu.clarin.sru.client.SRUVersion;
import eu.clarin.sru.fcs.aggregator.data.CenterRegistry;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.*;
import org.zkoss.util.media.AMedia;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Label;
import org.zkoss.zul.Menuitem;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treechildren;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Window;
import org.zkoss.zul.event.ZulEvents;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRURecord;
import eu.clarin.sru.client.SRUSearchRetrieveRequest;
import eu.clarin.sru.client.SRUSearchRetrieveResponse;
import eu.clarin.sru.client.SRUThreadedClient;
import eu.clarin.sru.client.fcs.ClarinFCSRecordData;
import eu.clarin.sru.fcs.aggregator.sparam2.SearchResult2;
import eu.clarin.sru.fcs.aggregator.sparam2.Corpus2;
import eu.clarin.sru.fcs.aggregator.sparam2.Corpus2Renderer;
import eu.clarin.sru.fcs.aggregator.sparam2.CorpusTreeModel2;
import eu.clarin.sru.fcs.aggregator.sparam2.Languages;
import eu.clarin.sru.fcs.aggregator.sparam2.SearchResult2;
import eu.clarin.sru.fcs.aggregator.sparam2.SearchResultRecordRenderer2;
import eu.clarin.sru.fcs.aggregator.sresult.SearchResultRecordRenderer;
import eu.clarin.sru.fcs.aggregator.sresult.SearchResultsController;
import eu.clarin.sru.fcs.aggregator.util.SRUCQLsearchRetrieve;
import eu.clarin.weblicht.wlfxb.tc.api.GeoLongLatFormat;
import eu.clarin.weblicht.wlfxb.tc.api.Token;
import eu.clarin.weblicht.wlfxb.tc.xb.TextCorpusStored;
import eu.clarin.weblicht.wlfxb.xb.WLData;
import java.util.concurrent.Future;
import javax.ws.rs.core.MediaType;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.DefaultTreeNode;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Popup;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.SimpleListModel;

/**
 *
 * @author Yana Panchenko
 */
public class SearchResults extends SelectorComposer<Component> {

    private static final Logger logger = Logger.getLogger(Aggregator.class.getName());
//    @Wire
//    private Grid anzeigeGrid;
    @Wire
    private Textbox searchString;
    @Wire
    private Combobox languageSelect;
//    @Wire
//    private Button searchButton;
//    @Wire
//    private Groupbox allCorpora;
//    @Wire
//    private Comboitem german;
    @Wire
    private Comboitem anyLanguage;
    @Wire
    private Window resultsBox;
//    @Wire
//    private Button selectAll;
//    @Wire
//    private Button deselectAll;
//    @Wire
//    private Window mainWindow;
    @Wire
    private Combobox maximumRecordsSelect;
//    @Wire
//    private Button addForeignEndpoint;
    @Wire
    Combobox foreignEndpointSelect;
    @Wire
    private Tree tree;
    @Wire
    private Label searchResultsProgress;
    @Wire
    private Popup wspaceSigninpop;
    @Wire
    private Textbox wspaceUserName;
    @Wire
    private Textbox wspaceUserPwd;
    private WebResource mapGenerator;
    public static final String MAPS_SERVICE_URL = "http://weblicht.sfs.uni-tuebingen.de/rws/service-geolocationconsumer/resources/geoloc/";
    private Map<String, List<String>> xAggregationContext;
    private SRUVersion version = SRUVersion.VERSION_1_2;
    private CenterRegistry registry;
    private boolean testingMode = false;
    
    private SRUThreadedClient searchClient;
    
    private List<SearchResult2> resultsUnprocessed;
    private List<SearchResult2> resultsProcessed;
    private UpdateResultsThread resultsThread;
    private int currentRequestId = 0;
    
    
    private Progressmeter progress;
    


    @Override
    public void doAfterCompose(Component comp) throws Exception {

        super.doAfterCompose(comp);
        
        Executions.getCurrent().getDesktop().enableServerPush(true);
        searchClient = (SRUThreadedClient) Executions.getCurrent().getDesktop().getWebApp().getAttribute(WebAppListener.SHARED_SRU_CLIENT);
        

//        processParameters();
//
//        languageSelect.setSelectedItem(anyLanguage);
//
//        searchResultsController = new SearchResultsController(resultsBox, searchResultsProgress);
//        // assign the search controller to desktop, so that it can be accessed to be shutdown when the desktop is destroyed
//        Executions.getCurrent().getDesktop().setAttribute(searchResultsController.getClass().getSimpleName(), searchResultsController);
//        // also add it to the list of actice controllers of the web application, so that they can be shutdown when the application stops
//        Set<SearchResultsController> activeControllers = (Set<SearchResultsController>) Executions.getCurrent().getDesktop().getWebApp().getAttribute(WebAppListener.ACTIVE_SEARCH_CONTROLLERS);
//        activeControllers.add(searchResultsController);
//
//        registry = new CenterRegistry();
//        registry.loadChildren(testingMode);
//        CorpusTreeModel corporaModel = new CorpusTreeModel(registry);
//        tree.setModel(corporaModel);
//        tree.setItemRenderer(new CorpusTreeNodeRenderer());
//        tree.setMultiple(true);


        //tempMap();
       
        
    }

    @Listen("onSelect = #languageSelect")
    public void onSelectLanguage(Event ev) {
        //TODO
    }

//    @Listen(ZulEvents.ON_AFTER_RENDER + "=#tree")
//    public void onAfterRenderCorporaTree(Event ev) {
//        Corpus2Renderer.selectEndpoints(this.tree, this.xAggregationContext);
//    }
//
//    @Listen("onClick = #selectAll")
//    public void onSelectAll(Event ev) {
//        Treechildren openTreeItems = tree.getTreechildren();
//        for (Treeitem openItem : openTreeItems.getItems()) {
//            Corpus2Renderer.selectItem(openItem);
//        }
//    }
//
//    @Listen("onClick = #deselectAll")
//    public void onDeselectAll(Event ev) {
//        Treechildren openTreeItems = tree.getTreechildren();
//        for (Treeitem openItem : openTreeItems.getItems()) {
//            Corpus2Renderer.unselectItem(openItem);
//        }
//    }

    @Listen("onClick=#clearResults")
    public void onClearResults(Event ev) {
        resultsBox.getChildren().clear();
    }

//    @Listen("onClick=#exportResultsCSV")
//    public void onExportResultsCSV(Event ev) {
//        searchResultsController.exportCSV();
//    }
//
//    @Listen("onClick=#exportResultsTCF")
//    public void onExportResultsTCF(Event ev) {
//        searchResultsController.exportTCF();
//    }
//
//    @Listen("onClick=#exportResultsPWTCF")
//    public void onExportResultsPWTCF(Event ev) {
//        wspaceSigninpop.open(resultsBox, "top_center");
//    }
//
//    @Listen("onClick=#wspaceSigninBtn")
//    public void onSignInExportResultsPWTCF(Event ev) {
//        String user = wspaceUserName.getValue();
//        String pswd = wspaceUserPwd.getValue();
//        if (user.isEmpty() || pswd.isEmpty()) {
//            Messagebox.show("Need user name and password!");
//        } else {
//            wspaceUserPwd.setValue("");
//            wspaceSigninpop.close();
//            searchResultsController.exportPWTCF(user, pswd);
//        }
//    }
//    
//    @Listen("onOK=#wspaceUserPwd")
//    public void onSignInExportResultsPWTCFPwdOK(Event ev) {
//        onSignInExportResultsPWTCF(ev);
//    }
//    
//    @Listen("onClick=#wspaceCancelBtn")
//    public void onSignInPWCancel(Event ev) {
//        wspaceUserPwd.setValue("");
//        wspaceSigninpop.close();
//    }


    private void processParameters() {

        String[] paramValue;
        String contextJson = null;

        String[] paramsReceived = new String[4];

        paramValue = Executions.getCurrent().getParameterMap().get("query");
        if (paramValue != null) {
            searchString.setValue(paramValue[0].trim());
            paramsReceived[0] = searchString.getValue();
        }
        paramValue = Executions.getCurrent().getParameterMap().get("operation");
        if (paramValue != null) {
            String operationString = paramValue[0].trim();
            paramsReceived[1] = operationString;
            if (!operationString.equals("searchRetrieve")) {
                Messagebox.show("Not supported operation " + operationString, "FCS", 0, Messagebox.INFORMATION);
            }
        }
        paramValue = Executions.getCurrent().getParameterMap().get("version");
        if (paramValue != null) {
            String versionString = paramValue[0].trim();
            paramsReceived[2] = versionString;
            if (versionString.equals("1.2")) {
                version = SRUVersion.VERSION_1_2;
            } else if (versionString.equals("1.1")) {
                version = SRUVersion.VERSION_1_1;
            } else {
                Messagebox.show("SRU Version " + version + " not supported", "FCS", 0, Messagebox.INFORMATION);
            }
        }
        paramValue = Executions.getCurrent().getParameterMap().get("x-aggregation-context");
        if (paramValue != null) {
            contextJson = paramValue[0].trim();
            paramsReceived[3] = contextJson;
        }
        logger.log(Level.INFO, "Received parameters: query[{0}], operation[{1}], version[{2}], x-aggregation-context[{3}], ", paramsReceived);

        paramValue = Executions.getCurrent().getParameterMap().get("mode");
        if (paramValue != null) {
            String mode = paramValue[0].trim();
            if (mode.equals("testing")) {
                testingMode = true;
            }
        }

        if (contextJson != null) {
            Gson gson = new Gson();
            Type mapType = new TypeToken<LinkedHashMap<String, ArrayList<String>>>() {
            }.getType();
            try {
                this.xAggregationContext = gson.fromJson(contextJson, mapType);
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Error parsing JSON from x-aggregation-context: {0} {1}", new String[]{ex.getMessage(), contextJson});
                Messagebox.show("Error in x-aggregation-context parameter", "FCS", 0, Messagebox.INFORMATION);
            }
        }

    }


    void executeSearch(Map<String, Set<Corpus2>> selectedCorpora, int maxRecords, String searchString, Progressmeter pMeter) {
        
        this.progress = pMeter;
        this.progress.setValue(0);
        this.progress.setVisible(true);
        
        
        // terminate previous search requests and corresponding response processing
        terminateProcessingRequestsAndResponses();

        // update current search request id
        currentRequestId++;

        // clear are where results are to be displayed
        resultsBox.getChildren().clear();

        // empty storage for unprocessed processed lists with recordsData
        resultsProcessed = new ArrayList<SearchResult2>();
        resultsUnprocessed = new ArrayList<SearchResult2>();

        // finally, send search requests to all the selected by user 
        // endpoints/corpora and process the responses
        for (String endpointUrl : selectedCorpora.keySet()) {
            for (Corpus2 corpus : selectedCorpora.get(endpointUrl)) {
                resultsUnprocessed.add(executeSearch(corpus, maxRecords, searchString));
            }
        }
        
        processResponses();

    }

    private SearchResult2 executeSearch(Corpus2 corpus, int maxRecords, String searchString) {
        SearchResult2 resultsItem = new SearchResult2(corpus);
        logger.log(Level.FINE, "Executing search for {0} query={1} maxRecords={2}", 
                new Object[]{corpus.toString(), searchString, maxRecords});
        SRUSearchRetrieveRequest searchRequest = new SRUSearchRetrieveRequest(corpus.getEndpointUrl());
        searchRequest.setVersion(version);
        searchRequest.setMaximumRecords(maxRecords);
        searchRequest.setRecordSchema(ClarinFCSRecordData.RECORD_SCHEMA);
        searchString = searchString.replace(" ", "%20");
        searchRequest.setQuery("%22" + searchString + "%22");
        if (resultsItem.hasCorpusHandler()) {
            searchRequest.setExtraRequestData(SRUCQLsearchRetrieve.CORPUS_HANDLE_PARAMETER, resultsItem.getCorpus().getHandle());
        }
        try {
            Future<SRUSearchRetrieveResponse> futureResponse = searchClient.searchRetrieve(searchRequest);
            resultsItem.setFutureResponse(futureResponse);
        } catch (SRUClientException ex) {
            logger.log(Level.SEVERE, "SearchRetrieve failed for {0} {1} {2}", 
                    new String[]{corpus.getEndpointUrl(), ex.getClass().getName(), ex.getMessage()});
        }
        return resultsItem;
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
                Logger.getLogger(SearchResults.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        Logger.getLogger(SearchResults.class.getName()).log(Level.INFO, "Search terminated");
    }

    private void processResponses() {
        resultsThread = new SearchResults.UpdateResultsThread();
        resultsThread.start();
    }
    
    
    private class UpdateResultsThread extends Thread {

        @Override
        public void run() {
            while (!resultsUnprocessed.isEmpty() && !Thread.currentThread().isInterrupted()) {
                
                SearchResult2 resultsItem = resultsUnprocessed.remove(0);
                if (!resultsItem.isWaitingForResponse()) {
                    resultsItem.consumeResponse();
                    Executions.schedule(resultsBox.getDesktop(), new SearchResults.ResponseListener(resultsItem, currentRequestId), new Event("onDummy"));
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
                for (SearchResult2 resultsItem : resultsUnprocessed) {
                    resultsItem.cancelWaitingForResponse();
                }
            }

        }
    }

    private class ResponseListener implements EventListener {

        int requestId;
        SearchResult2 resultsItem;

        public ResponseListener(SearchResult2 resultsItem, int requestId) {
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
                resultsBox.appendChild(groupbox);
            }

            // if in the meanwhile there was a new request
            // this search result is outdated, detach it:
            if (requestId != currentRequestId) {
                groupbox.detach();
            }
            
            if (resultsUnprocessed.isEmpty()) {
                progress.setValue(0);
                progress.setVisible(false);
            } else {
                progress.setValue(100 * resultsProcessed.size() / (resultsUnprocessed.size() + resultsProcessed.size() + 1));
            }

        }
    }
    
        private Groupbox createRecordsGroup(SearchResult2 resultsItem) {

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
        sb.append(resultsItem.getCorpus().getInstitution().getName());
        sb.append(" ");
        sb.append(resultsItem.getCorpus().getEndpointUrl());
        if (resultsItem.hasCorpusHandler()) {
            if (resultsItem.getCorpus().getDisplayName() != null) {
                sb.append(" ");
                sb.append(resultsItem.getCorpus().getDisplayName());
            }
            if (sb.append(resultsItem.getCorpus().getHandle()) != null) {
                sb.append(" ");
                sb.append(resultsItem.getCorpus().getHandle());
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
            grid.setRowRenderer(new SearchResultRecordRenderer2(resultsItem));
            recordsGroup.appendChild(grid);
        } else { // the response was fine, but there are no records
            recordsGroup.appendChild(new Label("Sorry, there were no results!"));
        }

        return recordsGroup;
    }
}
