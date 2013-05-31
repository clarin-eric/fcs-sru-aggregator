package eu.clarin.sru.fcs.aggregator.app;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.googlecode.sardine.Sardine;
import com.googlecode.sardine.SardineFactory;
import com.googlecode.sardine.impl.SardineException;
import eu.clarin.sru.client.SRUVersion;
import eu.clarin.sru.fcs.aggregator.data.CenterRegistry;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.*;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Window;
import com.sun.jersey.api.client.WebResource;
import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRURecord;
import eu.clarin.sru.client.SRUSearchRetrieveRequest;
import eu.clarin.sru.client.SRUSearchRetrieveResponse;
import eu.clarin.sru.client.SRUThreadedClient;
import eu.clarin.sru.client.fcs.ClarinFCSRecordData;
import eu.clarin.sru.client.fcs.DataViewKWIC;
import eu.clarin.sru.fcs.aggregator.sparam2.Corpus2;
import eu.clarin.sru.fcs.aggregator.sparam2.Kwic;
import eu.clarin.sru.fcs.aggregator.sparam2.SearchResult2;
import eu.clarin.sru.fcs.aggregator.sparam2.SearchResultRecordRenderer2;
import eu.clarin.sru.fcs.aggregator.sresult.SearchResultsController;
import eu.clarin.sru.fcs.aggregator.util.SRUCQLsearchRetrieve;
import eu.clarin.weblicht.wlfxb.io.WLDObjector;
import eu.clarin.weblicht.wlfxb.io.WLFormatException;
import eu.clarin.weblicht.wlfxb.md.xb.MetaData;
import eu.clarin.weblicht.wlfxb.tc.xb.TextCorpusStored;
import eu.clarin.weblicht.wlfxb.xb.WLData;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Popup;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Vlayout;

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
    //private Window resultsBox;
    private Vlayout resultsBox;
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
    
    
    //private Progressmeter progress;
    private ControlsVisibility controlsVisibility;
    private PagesVisibility pagesVisibility;
    
    
    private AtomicBoolean hasResults = new AtomicBoolean(false);
    private AtomicBoolean searchInProgress = new AtomicBoolean(false);
    
    private int[] searchOffset;
    private int maxRecords;

    
    @Wire
    private Window infoWin;
    
    private static final String WSPACE_SERVER_URL = "http://egi-cloud21.zam.kfa-juelich.de"; 
    private static final String WSPACE_WEBDAV_DIR = "/owncloud/remote.php/webdav/";
    private static final String WSPACE_AGGREGATOR_DIR = "aggregator_results/";

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


    public void clearResults() {
        // terminate previous search requests and corresponding response processing
        terminateProcessingRequestsAndResponses();
        this.controlsVisibility.disableControls1();
        this.controlsVisibility.disableControls2();
        resultsBox.getChildren().clear();
        this.searchInProgress.set(false);
        this.hasResults.set(false);
        
        
    }


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


    void executeSearch(Map<String, Set<Corpus2>> selectedCorpora, int maxRecords, String searchString, int[] searchOffset) {
        
        
        this.controlsVisibility.disableControls1();
        this.controlsVisibility.enableControls2();
        this.controlsVisibility.disablePrevButton();
        this.controlsVisibility.disableNextButton();
        this.controlsVisibility.enableProgressMeter(0);
        
        this.maxRecords = maxRecords;
        this.hasResults.set(false);
        this.searchInProgress.set(true);
        this.searchOffset = searchOffset;
        
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
                resultsUnprocessed.add(executeSearch(corpus, searchString));
            }
        }
        
        processResponses();

    }

    private SearchResult2 executeSearch(Corpus2 corpus, String searchString) {
        SearchResult2 resultsItem = new SearchResult2(corpus);
        logger.log(Level.FINE, "Executing search for {0} query={1} maxRecords={2}", 
                new Object[]{corpus.toString(), searchString, maxRecords});
        SRUSearchRetrieveRequest searchRequest = new SRUSearchRetrieveRequest(corpus.getEndpointUrl());
        searchRequest.setVersion(version);
        searchRequest.setMaximumRecords(maxRecords);
        searchRequest.setRecordSchema(ClarinFCSRecordData.RECORD_SCHEMA);
        searchString = searchString.replace(" ", "%20");
        searchRequest.setQuery("%22" + searchString + "%22");
        searchRequest.setStartRecord(searchOffset[0] + searchOffset[1]);
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

    public boolean hasResults() {
        return this.hasResults.get();
    }
    
        public boolean hasSearchInProgress() {
        return this.searchInProgress.get();
    }

    void setVisibilityControllers(PagesVisibility pagesVisibility, ControlsVisibility controlsVisibility) {
        this.pagesVisibility = pagesVisibility;
        this.controlsVisibility = controlsVisibility;
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
                boolean last = searchInProgress.getAndSet(false);
                hasResults.set(true);
                if (last) {
                    controlsVisibility.disableProgressMeter();
                    searchOffset[0] = searchOffset[0] + searchOffset[1];
                    searchOffset[1] = maxRecords;
                    pagesVisibility.openSearchResult();
                    
                    if (searchOffset[0] > 1) {
                        controlsVisibility.enablePrevButton();
                    }
                    controlsVisibility.enableNextButton();
                    controlsVisibility.enableControls1();
                    controlsVisibility.enableControls2();
                    
                }
            } else {
                controlsVisibility.updateProgressMeter(100 * resultsProcessed.size() / (resultsUnprocessed.size() + resultsProcessed.size() + 1));
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
//        sb.append(resultsItem.getCorpus().getInstitution().getName());
//        sb.append(" ");
//        sb.append(resultsItem.getCorpus().getEndpointUrl());
        if (resultsItem.hasCorpusHandler()) {
            if (resultsItem.getCorpus().getDisplayName() != null) {
//                sb.append(" ");
                sb.append(resultsItem.getCorpus().getDisplayName());
                sb.append(", ");
            }
//            if (sb.append(resultsItem.getCorpus().getHandle()) != null) {
//                sb.append(" ");
//                sb.append(resultsItem.getCorpus().getHandle());
//            }
        }
        sb.append(resultsItem.getCorpus().getInstitution().getName());
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
            // info column
            columns.appendChild(c);
            c = new Column();
            c.setHflex("min");
            columns.appendChild(c);
            grid.appendChild(columns);
            

            List<SRURecord> sruRecords = resultsItem.getResponse().getRecords();
            ListModel lmodel = new SimpleListModel(sruRecords);
            grid.setModel(lmodel);
            grid.setRowRenderer(new SearchResultRecordRenderer2(resultsItem));
            recordsGroup.appendChild(grid);
            grid.setStyle("margin:10px;border:0px;");
        } else { // the response was fine, but there are no records
            recordsGroup.appendChild(new Label("no results"));
        }
        return recordsGroup;
    }
        
        public void exportTCF() {

        boolean noResult = true;
        StringBuilder text = new StringBuilder();
        
        Set<String> resultsLangs = new HashSet<String>();

        if (resultsProcessed != null && !resultsProcessed.isEmpty()) {
            for (SearchResult2 result : resultsProcessed) {
                resultsLangs.addAll(result.getCorpus().getLanguages());
                for (Kwic kwic : result.getKwics()) {
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
            String resultsLang = "unknown";
            if (resultsLangs.size() == 1) {
                resultsLang = resultsLangs.iterator().next();
            }
            TextCorpusStored tc = new TextCorpusStored(resultsLang);
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
        
        public void exportCSV() {
            String csv = getExportCSV();
            if (csv != null) {
                Filedownload.save(csv.toString(), "text/plain", "ClarinDFederatedContentSearch.csv");
            }
        }
        
        public String getExportCSV() {

        boolean noResult = true;
        StringBuilder csv = new StringBuilder();
        if (resultsProcessed != null && !resultsProcessed.isEmpty()) {
            csv.append("\"");
            csv.append("LEFT CONTEXT");
            csv.append("\"");
            csv.append(",");
            csv.append("\"");
            csv.append("KEYWORD");
            csv.append("\"");
            csv.append(",");
            csv.append("\"");
            csv.append("RIGHT CONTEXT");
            csv.append("\"");
            csv.append(",");
            csv.append("\"");
            csv.append("PID");
            csv.append("\"");
            csv.append(",");
            csv.append("\"");
            csv.append("REFERENCE");
            csv.append("\"");
            csv.append("\n");
            
            for (SearchResult2 result : resultsProcessed) {
                for (Kwic kwic : result.getKwics()) {
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
                    csv.append(",");
                    csv.append("\"");
                    if (kwic.getPid() != null) {
                        csv.append(kwic.getPid().replace("\"", "QUOTE"));
                    }
                    csv.append("\"");
                    csv.append(",");
                    csv.append("\"");
                    if (kwic.getReference() != null) {
                        csv.append(kwic.getReference().replace("\"", "QUOTE"));
                    }
                    csv.append("\"");
                    csv.append("\n");
                    noResult = false;
                }
            }
        }

        if (noResult) {
            Messagebox.show("Nothing to export!");
            return null;
        } else {
            return csv.toString();
        }
        
        
    }
        
        
           
    
    public void exportPWTCF(String user, String pass) {
        String text = kwcToText();

        if (text.isEmpty()) {
            Messagebox.show("Nothing to export!");
        } else {
            WLData data;
            MetaData md = new MetaData();
            //data.metaData.source = "Tuebingen Uni";
            //md.addMetaDataItem("title", "binding test");
            //md.addMetaDataItem("author", "Yana");
            //TODO when language solution will working add specific languages/unknown...
            TextCorpusStored tc = new TextCorpusStored("de");
            tc.createTextLayer().addText(text.toString());
            data = new WLData(md, tc);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try {
                WLDObjector.write(data, os);
                //Filedownload.save(os.toByteArray(), "text/tcf+xml", "ClarinDFederatedContentSearch.xml");
                Sardine sardine = SardineFactory.begin();
                sardine.setCredentials(user, pass);
                String outputDir = WSPACE_SERVER_URL + WSPACE_WEBDAV_DIR + WSPACE_AGGREGATOR_DIR;
                if (!sardine.exists(outputDir)) {
                    sardine.createDirectory(outputDir);
                }
		Date currentDate = new Date();
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
                Random generator = new Random();
                int rn1 = generator.nextInt(1000000000);
                String createdFilePath = outputDir + format.format(currentDate) + "-" + rn1 + ".tcf";
                while (sardine.exists(createdFilePath)) {
                    rn1 = generator.nextInt(1000000000);
                    createdFilePath = outputDir + format.format(currentDate) + "-" + rn1 + ".tcf";
                }
                sardine.put(createdFilePath, os.toByteArray(), "text/tcf+xml");
                Messagebox.show("Export complete!\nCreated file:\n" + createdFilePath);
            } catch (SardineException ex) {
                Logger.getLogger(SearchResultsController.class.getName()).log(Level.SEVERE, "Error accessing " + WSPACE_SERVER_URL + WSPACE_WEBDAV_DIR, ex);
                Messagebox.show("Wrong name or password!");
            } catch (IOException ex) {
                Logger.getLogger(SearchResultsController.class.getName()).log(Level.SEVERE, "Error accessing " + WSPACE_SERVER_URL + WSPACE_WEBDAV_DIR, ex);
            } catch (WLFormatException ex) {
                logger.log(Level.SEVERE, "Error exporting TCF {0} {1}", new String[]{ex.getClass().getName(), ex.getMessage()});
                Messagebox.show("Sorry, export error!");
            }
        }
    }
    
    public void exportPWCSV(String user, String pass) {
        String csv = getExportCSV();
        if (csv != null) {
            try {
                Sardine sardine = SardineFactory.begin();
                sardine.setCredentials(user, pass);
                String outputDir = WSPACE_SERVER_URL + WSPACE_WEBDAV_DIR + WSPACE_AGGREGATOR_DIR;
                if (!sardine.exists(outputDir)) {
                    sardine.createDirectory(outputDir);
                }
		Date currentDate = new Date();
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
                Random generator = new Random();
                int rn1 = generator.nextInt(1000000000);
                String createdFilePath = outputDir + format.format(currentDate) + "-" + rn1 + ".csv";
                while (sardine.exists(createdFilePath)) {
                    rn1 = generator.nextInt(1000000000);
                    createdFilePath = outputDir + format.format(currentDate) + "-" + rn1 + ".csv";
                }
                sardine.put(createdFilePath, csv.getBytes(), "text/csv");
                Messagebox.show("Export complete!\nCreated file:\n" + createdFilePath);
            } catch (SardineException ex) {
                logger.log(Level.SEVERE, "Error accessing " + WSPACE_SERVER_URL + WSPACE_WEBDAV_DIR, ex);
                Messagebox.show("Wrong name or password!");
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Error exporting TCF {0} {1}", new String[]{ex.getClass().getName(), ex.getMessage()});
                Messagebox.show("Sorry, export error!");
            }
        }
    }
    
    
    
    private String kwcToText() {
        StringBuilder text = new StringBuilder();

        if (resultsProcessed != null && !resultsProcessed.isEmpty()) {
            for (SearchResult2 result : resultsProcessed) {
                for (Kwic kwic : result.getKwics()) {
                    text.append(kwic.getLeft());
                    text.append(" ");
                    text.append(kwic.getKeyword());
                    text.append(" ");
                    text.append(kwic.getRight());
                    text.append("\n");
                }
            }

        }
        return text.toString();
    }
}