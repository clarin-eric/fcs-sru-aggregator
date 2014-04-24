package eu.clarin.sru.fcs.aggregator.app;

import com.googlecode.sardine.Sardine;
import com.googlecode.sardine.SardineFactory;
import com.googlecode.sardine.impl.SardineException;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import eu.clarin.sru.client.SRUVersion;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.*;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Messagebox;
import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUSearchRetrieveRequest;
import eu.clarin.sru.client.SRUSearchRetrieveResponse;
import eu.clarin.sru.client.SRUThreadedClient;
import eu.clarin.sru.client.fcs.ClarinFCSRecordData;
import eu.clarin.sru.fcs.aggregator.sopt.Corpus;
import eu.clarin.sru.fcs.aggregator.sopt.Languages;
import eu.clarin.sru.fcs.aggregator.sresult.SearchResult;
import eu.clarin.sru.fcs.aggregator.sresult.SearchResultContent;
import eu.clarin.sru.fcs.aggregator.sresult.SearchResultGroupRenderer;
import eu.clarin.sru.fcs.aggregator.util.SRUCQL;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Vlayout;

/**
 * Class representing Search Results page. Displays results in a table-like
 * manner, allows to retrieve next/previous results, and provides export results
 * possibilities.
 *
 * @author Yana Panchenko
 */
public class SearchResults extends SelectorComposer<Component> {

    private static final Logger LOGGER = Logger.getLogger(SearchResults.class.getName());
    @Wire
    private Vlayout resultsBox;
    private SRUVersion version = SRUVersion.VERSION_1_2;
    private SRUThreadedClient searchClient;
    private List<SearchResult> resultsUnprocessed;
    private List<SearchResult> resultsProcessed;
    private UpdateResultsThread resultsThread;
    private int currentRequestId = 0;
    private ControlsVisibility controlsVisibility;
    private PagesVisibility pagesVisibility;
    private AtomicBoolean hasResults = new AtomicBoolean(false);
    private AtomicBoolean searchInProgress = new AtomicBoolean(false);
    //private int[] searchOffset = new int[2];
    private int startRecord;
    private int maxRecords;
    private static final String WSPACE_SERVER_URL = "http://egi-cloud21.zam.kfa-juelich.de";
    private static final String WSPACE_WEBDAV_DIR = "/owncloud/remote.php/webdav/";
    private static final String WSPACE_AGGREGATOR_DIR = "aggregator_results/";
    private Timer timer;
    private int seconds = 200;
    private String searchLanguage;
    private Languages languages;
    
    private static final String DROP_OFF_URL = "http://ws1-clarind.esc.rzg.mpg.de/drop-off/storage/";

    private Groupbox firstNoResultsGb = null;
    private final Object firstNoResultsGbLoc = new Object();
    
    private SearchResultGroupRenderer srItemRenderer;
    private SearchResultContent srExporter;
    
    private static final String SEARCH_RESULTS_ENCODING = "UTF-8";

    @Override
    public void doAfterCompose(Component comp) throws Exception {

        super.doAfterCompose(comp);
        languages = (Languages) Executions.getCurrent().getDesktop().getWebApp().getAttribute(WebAppListener.LANGUAGES);
        setUpSRUVersion();
        Executions.getCurrent().getDesktop().enableServerPush(true);
        searchClient = (SRUThreadedClient) Executions.getCurrent().getDesktop().getWebApp().getAttribute(WebAppListener.SHARED_SRU_CLIENT);
        // assign the search controller to desktop, so that it can be accessed to be shutdown when the desktop is destroyed
        Executions.getCurrent().getDesktop().setAttribute(this.getClass().getSimpleName(), this);
        // also add it to the list of actice controllers of the web application, so that they can be shutdown when the application stops
        Set<SearchResults> activeControllers = (Set<SearchResults>) Executions.getCurrent().getDesktop().getWebApp().getAttribute(WebAppListener.ACTIVE_SEARCH_CONTROLLERS);
        activeControllers.add(this);
        
        srItemRenderer = new SearchResultGroupRenderer();
        srExporter = new SearchResultContent();
    }

    public void clearResults() {
        // terminate previous search requests and corresponding response processing
        terminateProcessingRequestsAndResponses();
        this.controlsVisibility.disableControls1();
        this.controlsVisibility.disableControls2();
        resultsBox.getChildren().clear();
        synchronized (firstNoResultsGbLoc) {
        firstNoResultsGb = null;
        }
        this.searchInProgress.set(false);
        this.hasResults.set(false);
    }

    void executeSearch(Map<String, Set<Corpus>> selectedCorpora, int startRecord, int maxRecords, String searchString, String searchLanguage) {

        this.controlsVisibility.disableControls1();
        this.controlsVisibility.enableControls2();
        this.controlsVisibility.disablePrevButton();
        this.controlsVisibility.disableNextButton();
        this.controlsVisibility.enableProgressMeter(0);

        this.searchLanguage = searchLanguage;
        this.maxRecords = maxRecords;
        this.hasResults.set(false);
        this.searchInProgress.set(true);
        this.startRecord = startRecord;

        // terminate previous search requests and corresponding response processing
        terminateProcessingRequestsAndResponses();

        // update current search request id
        currentRequestId++;

        // clear area where results are to be displayed
        resultsBox.getChildren().clear();

        // empty storage for unprocessed processed lists with recordsData
        resultsProcessed = new ArrayList<SearchResult>();
        resultsUnprocessed = new ArrayList<SearchResult>();
        synchronized (firstNoResultsGbLoc) {
        firstNoResultsGb = null;
        }
        
        // set up timer, so that when it takes too much time to get the response to cancell the request
        timer = new Timer();
        timer.schedule(new TimeoutTask(), seconds * 1000);

        // finally, send search requests to all the selected by user 
        // endpoints/corpora and process the responses
        for (String endpointUrl : selectedCorpora.keySet()) {
            for (Corpus corpus : selectedCorpora.get(endpointUrl)) {
                resultsUnprocessed.add(executeSearch(corpus, searchString));
            }
        }

        processResponses();
    }

    private SearchResult executeSearch(Corpus corpus, String searchString) {
        SearchResult resultsItem = new SearchResult(corpus, searchString, startRecord, startRecord + maxRecords - 1);
        LOGGER.log(Level.INFO, "Executing search for {0} query={1} maxRecords={2}",
                new Object[]{corpus.toString(), searchString, maxRecords});
        SRUSearchRetrieveRequest searchRequest = new SRUSearchRetrieveRequest(corpus.getEndpointUrl());
        searchRequest.setVersion(version);
        searchRequest.setMaximumRecords(maxRecords);
        searchRequest.setRecordSchema(ClarinFCSRecordData.RECORD_SCHEMA);
        //searchString = searchString.replace(" ", "%20");
        //searchRequest.setQuery("%22" + searchString + "%22");
        searchRequest.setQuery("\"" + searchString + "\"");
        //searchRequest.setStartRecord(searchOffset[0] + searchOffset[1]);
        searchRequest.setStartRecord(this.startRecord);
        if (resultsItem.hasCorpusHandler()) {
            searchRequest.setExtraRequestData(SRUCQL.SEARCH_CORPUS_HANDLE_PARAMETER, resultsItem.getCorpus().getHandle());
        }
        try {
            Future<SRUSearchRetrieveResponse> futureResponse = searchClient.searchRetrieve(searchRequest);
            resultsItem.setFutureResponse(futureResponse);
        } catch (SRUClientException ex) {
            LOGGER.log(Level.SEVERE, "SearchRetrieve failed for {0} {1} {2}",
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
            timer.cancel();
        }
        LOGGER.log(Level.INFO, "Search terminated");
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

                SearchResult resultsItem = resultsUnprocessed.remove(0);
                if (!resultsItem.isWaitingForResponse()) {
                    resultsItem.consumeResponse();
                    Executions.schedule(resultsBox.getDesktop(), new SearchResults.ResponseListener(srItemRenderer, resultsItem, currentRequestId), new Event("onDummy"));
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
        SearchResultGroupRenderer srRenderer;

        public ResponseListener(SearchResultGroupRenderer srRenderer, SearchResult resultsItem, int requestId) {
            this.srRenderer = srRenderer;
            this.resultsItem = resultsItem;
            this.requestId = requestId;
        }

        @Override
        public void onEvent(Event event) {

            // create groupbox with search results item
            //Groupbox groupbox = createRecordsGroup(resultsItem);
            Groupbox groupbox = this.srRenderer.createRecordsGroup(resultsItem);

            // append this search result only if it
            // is a result of the current request
            if (requestId == currentRequestId) {
                // if the item has results, append it before
                // the item that has no resuls (if exists):
                if (groupbox.getAttribute(SearchResultGroupRenderer.NO_RESULTS) == null) {
                    resultsBox.insertBefore(groupbox, firstNoResultsGb);
                // if there are no results in this item,
                // append it a the end:
                } else {
                    resultsBox.appendChild(groupbox);
                    synchronized (firstNoResultsGbLoc) {
                        if (firstNoResultsGb == null) {
                            firstNoResultsGb = groupbox;
                        }
                    }
                }

            }

            // if in the meanwhile there was a new request
            // this search result is outdated, detach it:
            if (requestId != currentRequestId) {
                groupbox.detach();
                synchronized (firstNoResultsGbLoc) {
                    firstNoResultsGb = null;
                }
            }

            if (resultsUnprocessed.isEmpty()) {
                boolean last = searchInProgress.getAndSet(false);
                hasResults.set(true);
                if (last) {
                    controlsVisibility.disableProgressMeter();
                    pagesVisibility.openSearchResult();

                    if (startRecord > 1) {
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


    
    public void exportTCF() {
            byte[] bytes = srExporter.getExportTokenizedTCF(resultsProcessed, searchLanguage, languages);
            if (bytes != null) {
                Filedownload.save(bytes, "text/tcf+xml", "ClarinDFederatedContentSearch.xml");
        }
    }
    
    public void exportText() {
            String text = srExporter.getExportText(resultsProcessed);
            if (text != null) {
                Filedownload.save(text, "text/plain", "ClarinDFederatedContentSearch.txt");
        }
    }
    
    
    void exportExcel() {
        
        byte[] bytes = srExporter.getExportExcel(resultsProcessed);
            if (bytes != null) {
                Filedownload.save(bytes, "text/tcf+xml", "ClarinDFederatedContentSearch.xls");
        }
    }

  

    void exportPWText(String user, String pass) {
        byte[] bytes = null;
        try {
            String text = srExporter.getExportText(resultsProcessed);
            if (text != null) {
                bytes = text.getBytes(SEARCH_RESULTS_ENCODING);
            }
        } catch (Exception ex) {
            Logger.getLogger(SearchResults.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (bytes != null) {
            uploadToPW(user, pass, bytes, "text/plan",".txt");
        }
    }
    
    String useWebLichtOnText() {
        String url = null;
        try {
            String text = srExporter.getExportText(resultsProcessed);
            if (text != null) {
                byte[] bytes = text.getBytes(SEARCH_RESULTS_ENCODING);
                url = uploadToDropOff(bytes, "text/plan",".txt");
            }
        } catch (Exception ex) {
            Logger.getLogger(SearchResults.class.getName()).log(Level.SEVERE, null, ex);
        }
        return url;
    }
    
    String useWebLichtOnToks() {
        String url = null;
        byte[] bytes = srExporter.getExportTokenizedTCF(resultsProcessed, searchLanguage, languages);
        if (bytes != null) {
            url = uploadToDropOff(bytes, "text/tcf+xml",".tcf");
        }
        return url;
    }

    void exportPWExcel(String user, String pass) {
        byte[] bytes = srExporter.getExportExcel(resultsProcessed);
            if (bytes != null) {
                uploadToPW(user, pass, bytes, "application/vnd.ms-excel",".xls");
        }
    }
    
   public void exportPWTCF(String user, String pass) {
       
       byte[] bytes = srExporter.getExportTokenizedTCF(resultsProcessed, searchLanguage, languages);
            if (bytes != null) {
                uploadToPW(user, pass, bytes, "text/tcf+xml",".tcf");
        }
    }

    
    public void exportCSV() {
        String csv = srExporter.getExportCSV(resultsProcessed, ";");
        if (csv != null) {
            Filedownload.save(csv.toString(), "text/plain", "ClarinDFederatedContentSearch.csv");
        }
    }
    
    
    public void exportPWCSV(String user, String pass) {
        //String csv = getExportCSV(";");
        String csv = srExporter.getExportCSV(resultsProcessed, ";");
        if (csv != null) {
            uploadToPW(user, pass, csv.getBytes(), "text/csv",".csv");
        }
    }
    
        private void uploadToPW(String user, String pass, byte[] bytes, String mimeType, String fileExtention) {
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
                String createdFilePath = outputDir + format.format(currentDate) + "-" + rn1 + fileExtention;
                while (sardine.exists(createdFilePath)) {
                    rn1 = generator.nextInt(1000000000);
                    createdFilePath = outputDir + format.format(currentDate) + "-" + rn1 + fileExtention;
                }
                sardine.put(createdFilePath, bytes, mimeType);
                Messagebox.show("Export complete!\nCreated file:\n" + createdFilePath);
            } catch (SardineException ex) {
                LOGGER.log(Level.SEVERE, "Error accessing " + WSPACE_SERVER_URL + WSPACE_WEBDAV_DIR, ex);
                Messagebox.show("Wrong name or password!");
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Error exporting {0} {1} {2}", new String[]{fileExtention, ex.getClass().getName(), ex.getMessage()});
                Messagebox.show("Sorry, export error!");
            }
    }
    
        private String uploadToDropOff(byte[] bytes, String mimeType, String fileExtention) {
            Client client = null;
            String url = null;
        try {

                Date currentDate = new Date();
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
                Random generator = new Random();
                int rn1 = generator.nextInt(1000000000);
                String createdFileName = format.format(currentDate) + "-" + rn1 + fileExtention;
    
                ClientConfig config = new DefaultClientConfig();
                client = Client.create(config);
                url = DROP_OFF_URL + createdFileName;
                WebResource service = client.resource(url);
        
            ClientResponse response = service.type(mimeType) //.accept(MediaType.TEXT_PLAIN).post(String.class, media.getStringData());
                    .post(ClientResponse.class, bytes);
            if (response.getClientResponseStatus() != ClientResponse.Status.CREATED) {
                    LOGGER.log(Level.SEVERE, "Error uploading {0}", new String[]{url});
                    Messagebox.show("Sorry, export to drop-off error!");
                    return null;
                }
        } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Error uploading {0} {1} {2}", new String[]{url, ex.getClass().getName(), ex.getMessage()});
                Messagebox.show("Sorry, export to drop-off error!");
                return null;
        } finally {
            if (client != null) {
            client.destroy();
            }
        }
        return url;

    }


    private void setUpSRUVersion() {
        String[] paramValue = Executions.getCurrent().getParameterMap().get(SRUCQL.VERSION);
        String versionString = null;
        if (paramValue != null) {
            versionString = paramValue[0].trim();
            if (versionString.equals("1.2")) {
                version = SRUVersion.VERSION_1_2;
            } else if (versionString.equals("1.1")) {
                version = SRUVersion.VERSION_1_1;
            } else {
                Messagebox.show("SRU Version " + version + " not supported", "FCS", 0, Messagebox.INFORMATION);
            }
        }
        LOGGER.log(Level.INFO, "Received parameter: {0}[{1}], ", new String[]{SRUCQL.VERSION,versionString});
    }
    
    /**
     * Class to control the waiting time for searchRetrieve response. If the response
     * doesn't come in predefined time limit, the response is no longer waited for.
     */
    class TimeoutTask extends TimerTask {

        @Override
        public void run() {
            SearchResult[] timeoutResult = new SearchResult[resultsUnprocessed.size() + 1];
            resultsUnprocessed.toArray(timeoutResult);
            for (int i = 0; i < timeoutResult.length; i++) {
                SearchResult resultsItem = timeoutResult[i];
                if (resultsItem != null) {
                    System.out.println(resultsItem.getCorpus().getInstitution().getName());
                    System.out.println(resultsItem.getCorpus().getEndpointUrl());
                    System.out.println("Timer out!!!");
                    resultsItem.cancelWaitingForResponse();
                }
            }
            timer.cancel(); //Terminate the timer thread
        }
    }
}