package eu.clarin.sru.fcs.aggregator.app;

import com.googlecode.sardine.Sardine;
import com.googlecode.sardine.SardineFactory;
import com.googlecode.sardine.impl.SardineException;
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
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRURecord;
import eu.clarin.sru.client.SRUSearchRetrieveRequest;
import eu.clarin.sru.client.SRUSearchRetrieveResponse;
import eu.clarin.sru.client.SRUThreadedClient;
import eu.clarin.sru.client.fcs.ClarinFCSRecordData;
import eu.clarin.sru.fcs.aggregator.sopt.Corpus;
import eu.clarin.sru.fcs.aggregator.sopt.Languages;
import eu.clarin.sru.fcs.aggregator.sresult.Kwic;
import eu.clarin.sru.fcs.aggregator.sresult.SearchResult;
import eu.clarin.sru.fcs.aggregator.sresult.SearchResultRecordRenderer;
import eu.clarin.sru.fcs.aggregator.util.SRUCQLsearchRetrieve;
import eu.clarin.weblicht.wlfxb.io.WLDObjector;
import eu.clarin.weblicht.wlfxb.io.WLFormatException;
import eu.clarin.weblicht.wlfxb.md.xb.MetaData;
import eu.clarin.weblicht.wlfxb.tc.api.MatchedCorpus;
import eu.clarin.weblicht.wlfxb.tc.api.Token;
import eu.clarin.weblicht.wlfxb.tc.xb.TextCorpusStored;
import eu.clarin.weblicht.wlfxb.xb.WLData;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.SimpleListModel;
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
    private int[] searchOffset;
    private int maxRecords;
    private static final String WSPACE_SERVER_URL = "http://egi-cloud21.zam.kfa-juelich.de";
    private static final String WSPACE_WEBDAV_DIR = "/owncloud/remote.php/webdav/";
    private static final String WSPACE_AGGREGATOR_DIR = "aggregator_results/";
    private Timer timer;
    private int seconds = 200;
    private String searchLanguage;
    private Languages languages;


    @Override
    public void doAfterCompose(Component comp) throws Exception {

        super.doAfterCompose(comp);
        languages = (Languages) Executions.getCurrent().getDesktop().getWebApp().getAttribute(WebAppListener.LANGUAGES);
        setUpSRUVersion();
        Executions.getCurrent().getDesktop().enableServerPush(true);
        searchClient = (SRUThreadedClient) Executions.getCurrent().getDesktop().getWebApp().getAttribute(WebAppListener.SHARED_SRU_CLIENT);
        setUpSRUVersion();
        // assign the search controller to desktop, so that it can be accessed to be shutdown when the desktop is destroyed
        Executions.getCurrent().getDesktop().setAttribute(this.getClass().getSimpleName(), this);
        // also add it to the list of actice controllers of the web application, so that they can be shutdown when the application stops
        Set<SearchResults> activeControllers = (Set<SearchResults>) Executions.getCurrent().getDesktop().getWebApp().getAttribute(WebAppListener.ACTIVE_SEARCH_CONTROLLERS);
        activeControllers.add(this);
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

    void executeSearch(Map<String, Set<Corpus>> selectedCorpora, int maxRecords, String searchString, int[] searchOffset, String searchLanguage) {

        this.controlsVisibility.disableControls1();
        this.controlsVisibility.enableControls2();
        this.controlsVisibility.disablePrevButton();
        this.controlsVisibility.disableNextButton();
        this.controlsVisibility.enableProgressMeter(0);

        this.searchLanguage = searchLanguage;
        this.maxRecords = maxRecords;
        this.hasResults.set(false);
        this.searchInProgress.set(true);
        this.searchOffset = searchOffset;

        // terminate previous search requests and corresponding response processing
        terminateProcessingRequestsAndResponses();

        // update current search request id
        currentRequestId++;

        // clear area where results are to be displayed
        resultsBox.getChildren().clear();

        // empty storage for unprocessed processed lists with recordsData
        resultsProcessed = new ArrayList<SearchResult>();
        resultsUnprocessed = new ArrayList<SearchResult>();

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
        SearchResult resultsItem = new SearchResult(corpus, searchString);
        LOGGER.log(Level.INFO, "Executing search for {0} query={1} maxRecords={2}",
                new Object[]{corpus.toString(), searchString, maxRecords});
        SRUSearchRetrieveRequest searchRequest = new SRUSearchRetrieveRequest(corpus.getEndpointUrl());
        searchRequest.setVersion(version);
        searchRequest.setMaximumRecords(maxRecords);
        searchRequest.setRecordSchema(ClarinFCSRecordData.RECORD_SCHEMA);
        //searchString = searchString.replace(" ", "%20");
        //searchRequest.setQuery("%22" + searchString + "%22");
        searchRequest.setQuery("\"" + searchString + "\"");
        searchRequest.setStartRecord(searchOffset[0] + searchOffset[1]);
        if (resultsItem.hasCorpusHandler()) {
            searchRequest.setExtraRequestData(SRUCQLsearchRetrieve.CORPUS_HANDLE_PARAMETER, resultsItem.getCorpus().getHandle());
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

    private Groupbox createRecordsGroup(SearchResult resultsItem) {

        Groupbox recordsGroup = new Groupbox();
        recordsGroup.setMold("3d");
        recordsGroup.setSclass("ccsLightBlue");
        recordsGroup.setContentStyle("border:0;");
        recordsGroup.setStyle("margin:10px;10px;10px;10px;");
        recordsGroup.setClosable(true);
        // create title
        StringBuilder sb = new StringBuilder();
        if (resultsItem.hasCorpusHandler()) {
            if (resultsItem.getCorpus().getDisplayName() != null) {
//                sb.append(" ");
                sb.append(resultsItem.getCorpus().getDisplayName());
                sb.append(", ");
            }
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
            grid.setRowRenderer(new SearchResultRecordRenderer(resultsItem));
            recordsGroup.appendChild(grid);
            grid.setStyle("margin:10px;border:0px;");
        } else { // the response was fine, but there are no records
            recordsGroup.appendChild(new Label("no results"));
        }
        return recordsGroup;
    }
    
    public void exportTCF() {
            byte[] bytes = getExportTokenizedTCF();
            if (bytes != null) {
                Filedownload.save(bytes, "text/tcf+xml", "ClarinDFederatedContentSearch.xml");
        }
    }
    
    public void exportText() {
            String text = getExportText().toString();
            if (text != null) {
                Filedownload.save(text, "text/plain", "ClarinDFederatedContentSearch.txt");
        }
    }
    
    
    void exportExcel() {
        
        byte[] bytes = getExportExcel();
            if (bytes != null) {
                Filedownload.save(bytes, "text/tcf+xml", "ClarinDFederatedContentSearch.xls");
        }
    }

    private byte[] getExportExcel() {
        
        boolean noResult = true;
        SXSSFWorkbook workbook = null;
        ByteArrayOutputStream excelStream = new ByteArrayOutputStream();
        if (resultsProcessed != null && !resultsProcessed.isEmpty()) {
            try {
                String[] headers = new String[] {
                    "LEFT CONTEXT", "KEYWORD", "RIGHT CONTEXT", "PID", "REFERENCE"};
            
                workbook = new SXSSFWorkbook();
                Sheet sheet = workbook.createSheet();

                Font boldFont = workbook.createFont();
                boldFont.setBoldweight(Font.BOLDWEIGHT_BOLD);

                // Header
                CellStyle headerStyle = workbook.createCellStyle();
                headerStyle.setFont(boldFont);

                Row row = sheet.createRow(0);

                for (int j = 0; j < headers.length; ++j) {
                    Cell cell = row.createCell(j, Cell.CELL_TYPE_STRING);
                    cell.setCellValue(headers[j]);
                    cell.setCellStyle(headerStyle);
                }

                // Body
                Cell cell;
                for (int k = 0; k < resultsProcessed.size(); k++) {
                    SearchResult result = resultsProcessed.get(k);
                    List<Kwic> kwics = result.getKwics();
                    for (int i = 0; i < kwics.size(); i++) {
                        Kwic kwic = kwics.get(i);
                        row = sheet.createRow(k + i + 1);
                        cell = row.createCell(0, Cell.CELL_TYPE_STRING);
                        cell.setCellValue(kwic.getLeft());
                        cell = row.createCell(1, Cell.CELL_TYPE_STRING);
                        cell.setCellValue(kwic.getKeyword());
                        cell = row.createCell(2, Cell.CELL_TYPE_STRING);
                        cell.setCellValue(kwic.getRight());
                        if (kwic.getPid() != null) {
                            cell = row.createCell(3, Cell.CELL_TYPE_STRING);
                            cell.setCellValue(kwic.getPid());
                        }
                        if (kwic.getReference() != null) {
                            cell = row.createCell(3, Cell.CELL_TYPE_STRING);
                            cell.setCellValue(kwic.getReference());
                        }
                        noResult = false;
                    }
                }
                workbook.write(excelStream);
            } catch (IOException ex) {
                // should not happen
                Logger.getLogger(SearchResults.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                if (workbook != null) {
                    workbook.dispose();
                }
            }
        }
        if (noResult) {
            Messagebox.show("Nothing to export!");
            return null;
        } else {
            return excelStream.toByteArray();
        }
        
    }

    void exportPWText(String user, String pass) {
        byte[] bytes = null;
        try {
            bytes = getExportText().toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(SearchResults.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (bytes != null) {
            uploadToPW(user, pass, bytes, "text/plan",".txt");
        }
    }

    void exportPWExcel(String user, String pass) {
        byte[] bytes = getExportExcel();
            if (bytes != null) {
                uploadToPW(user, pass, bytes, "application/vnd.ms-excel",".xls");
        }
    }
    
   public void exportPWTCF(String user, String pass) {
       
       byte[] bytes = getExportTokenizedTCF();
            if (bytes != null) {
                uploadToPW(user, pass, bytes, "text/tcf+xml",".tcf");
        }
    }
    
    private byte[] getExportTCF() {
        StringBuilder text = new StringBuilder();
        Set<String> resultsLangs = new HashSet<String>();
        if (resultsProcessed != null && !resultsProcessed.isEmpty()) {
            for (SearchResult result : resultsProcessed) {
                resultsLangs.addAll(result.getCorpus().getLanguages());
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
        ByteArrayOutputStream os = null;
        if (text.length() == 0) {
            Messagebox.show("Nothing to export!");
        } else {
            WLData data;
            MetaData md = new MetaData();
            String resultsLang = "unknown";
            if (resultsLangs.size() == 1) {
                resultsLang = resultsLangs.iterator().next();
                String code2 = languages.code2ForCode(resultsLang);
                if (code2 != null) {
                    resultsLang = code2;
                }
            } else if (!searchLanguage.equals("anylang")) {
                String code2 = languages.code2ForCode(searchLanguage);
                if (code2 == null) {
                    resultsLang = searchLanguage;
                } else {
                    resultsLang = code2;
                }
            }
            TextCorpusStored tc = new TextCorpusStored(resultsLang);
            tc.createTextLayer().addText(text.toString());
            data = new WLData(md, tc);
            os = new ByteArrayOutputStream();
            try {
                WLDObjector.write(data, os);
            } catch (WLFormatException ex) {
                LOGGER.log(Level.SEVERE, "Error exporting TCF {0} {1}", new String[]{ex.getClass().getName(), ex.getMessage()});
                Messagebox.show("Sorry, export error!");
            }
        }
        if (os == null) {
            return null;
        } else {
            return os.toByteArray();
        }
    }
    
    
        private byte[] getExportTokenizedTCF() {
        StringBuilder text = new StringBuilder();
        Set<String> resultsLangs = new HashSet<String>();
        if (resultsProcessed != null && !resultsProcessed.isEmpty()) {
            for (SearchResult result : resultsProcessed) {
                resultsLangs.addAll(result.getCorpus().getLanguages());
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
        ByteArrayOutputStream os = null;
        if (text.length() == 0) {
            Messagebox.show("Nothing to export!");
        } else {
            WLData data;
            MetaData md = new MetaData();
            String resultsLang = "unknown";
            if (resultsLangs.size() == 1) {
                resultsLang = resultsLangs.iterator().next();
                String code2 = languages.code2ForCode(resultsLang);
                if (code2 != null) {
                    resultsLang = code2;
                }
            } else if (!searchLanguage.equals("anylang")) {
                String code2 = languages.code2ForCode(searchLanguage);
                if (code2 == null) {
                    resultsLang = searchLanguage;
                } else {
                    resultsLang = code2;
                }
            }
            TextCorpusStored tc = new TextCorpusStored(resultsLang);
            tc.createTextLayer().addText(text.toString());
            addTokensSentencesMatches(tc);
            data = new WLData(md, tc);
            os = new ByteArrayOutputStream();
            try {
                WLDObjector.write(data, os);
            } catch (WLFormatException ex) {
                LOGGER.log(Level.SEVERE, "Error exporting TCF {0} {1}", new String[]{ex.getClass().getName(), ex.getMessage()});
                Messagebox.show("Sorry, export error!");
            }
        }
        if (os == null) {
            return null;
        } else {
            return os.toByteArray();
        }
    }
    
        
    private void addTokensSentencesMatches(TextCorpusStored tc) {
        
        TokenizerModel model = (TokenizerModel) Executions.getCurrent().getDesktop().getWebApp().getAttribute(WebAppListener.DE_TOK_MODEL);
        
        if (model == null || !tc.getLanguage().equals("de")) {
            return;
        }
        TokenizerME tokenizer = new TokenizerME(model);
        
        if (resultsProcessed != null && !resultsProcessed.isEmpty()) {
            tc.createTokensLayer();
            tc.createSentencesLayer();
            tc.createMatchesLayer("FCS", resultsProcessed.get(0).getSearchString());
            for (SearchResult result : resultsProcessed) {
                MatchedCorpus mCorpus = tc.getMatchesLayer().addCorpus(result.getCorpus().getDisplayName(), result.getCorpus().getHandle());
                for (Kwic kwic : result.getKwics()) {
                    List<Token> tokens = new ArrayList<Token>();
                    addToTcfTokens(tokens, tc, tokenizer.tokenize(kwic.getLeft()));
                    String[] target = tokenizer.tokenize(kwic.getKeyword());
                    List<Token> targetTokens = addToTcfTokens(tokens, tc, target);
                    addToTcfTokens(tokens, tc, tokenizer.tokenize(kwic.getRight()));
                    tc.getSentencesLayer().addSentence(tokens);
                    List<String> pidAndRef = new ArrayList<String>();
                    if (kwic.getPid() != null) {
                        pidAndRef.add(kwic.getPid());
                    }
                    if (kwic.getReference() != null) {
                        pidAndRef.add(kwic.getReference());
                    }
                    tc.getMatchesLayer().addItem(mCorpus, targetTokens, pidAndRef);
                }
            }
        }
    }
    
    
    private List<Token> addToTcfTokens(List<Token> tokens, TextCorpusStored tc, String[] tokenStrings) {
        List<Token> addedTokens = new ArrayList<Token>(tokenStrings.length);
        for (String tokenString : tokenStrings) {
            Token token = tc.getTokensLayer().addToken(tokenString);
            addedTokens.add(token);
            tokens.add(token);
        }
        return addedTokens;
    }
    
    private CharSequence getExportText() {
        StringBuilder text = new StringBuilder();
        //Set<String> resultsLangs = new HashSet<String>();
        if (resultsProcessed != null && !resultsProcessed.isEmpty()) {
            for (SearchResult result : resultsProcessed) {
                //resultsLangs.addAll(result.getCorpus().getLanguages());
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
        if (text.length() == 0) {
            Messagebox.show("Nothing to export!");
            return null;
        } else {
            return text;
        }
    }
    
    public void exportCSV() {
        String csv = getExportCSV(";");
        if (csv != null) {
            Filedownload.save(csv.toString(), "text/plain", "ClarinDFederatedContentSearch.csv");
        }
    }
    
    
    public void exportPWCSV(String user, String pass) {
        String csv = getExportCSV(";");
        if (csv != null) {
            uploadToPW(user, pass, csv.getBytes(), "text/csv",".csv");
        }
    }

    private String getExportCSV(String separator) {

        boolean noResult = true;
        StringBuilder csv = new StringBuilder();
        if (resultsProcessed != null && !resultsProcessed.isEmpty()) {
            String[] headers = new String[] {
                    "LEFT CONTEXT", "KEYWORD", "RIGHT CONTEXT", "PID", "REFERENCE"};
            for (String header : headers) {
                csv.append("\""); csv.append(header); csv.append("\"");
                csv.append(separator);
            }
            csv.append("\n");

            for (SearchResult result : resultsProcessed) {
                for (Kwic kwic : result.getKwics()) {
                    csv.append("\""); csv.append(escapeQuotes(kwic.getLeft())); csv.append("\"");
                    csv.append(separator);
                    csv.append("\""); csv.append(escapeQuotes(kwic.getKeyword())); csv.append("\"");
                    csv.append(separator);
                    csv.append("\""); csv.append(escapeQuotes(kwic.getRight())); csv.append("\"");
                    csv.append(separator);
                    csv.append("\"");
                    if (kwic.getPid() != null) {
                        csv.append(escapeQuotes(kwic.getPid()));
                    }
                    csv.append("\"");
                    csv.append(separator);
                    csv.append("\"");
                    if (kwic.getReference() != null) {
                        csv.append(escapeQuotes(kwic.getReference()));
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
    
    private CharSequence escapeQuotes(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '"') {
                sb.append('"');
            }
            sb.append(ch);
        }
        return sb;
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

    private void setUpSRUVersion() {
        String[] paramValue = Executions.getCurrent().getParameterMap().get("version");
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
        LOGGER.log(Level.INFO, "Received parameter: version[{0}], ", versionString);
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