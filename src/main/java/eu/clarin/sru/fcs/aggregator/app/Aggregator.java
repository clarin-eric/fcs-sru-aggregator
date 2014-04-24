package eu.clarin.sru.fcs.aggregator.app;

import java.util.Map;
import java.util.Set;
import java.util.logging.*;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;
import eu.clarin.sru.fcs.aggregator.sopt.Corpus;
import eu.clarin.sru.fcs.aggregator.sopt.Languages;
import eu.clarin.sru.fcs.aggregator.util.SRUCQL;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.zkoss.zul.A;
import org.zkoss.zul.Div;
import org.zkoss.zul.Menubar;
import org.zkoss.zul.Menuitem;
import org.zkoss.zul.North;
import org.zkoss.zul.Popup;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.South;

/**
 * Main component of the Aggregator application intended to provide
 * users access to CLARIN-FCS resources.
 * 
 * The webapp base URL corresponds to the default behavior of displaying
 * the main aggregator page, where the user can enter query, select the 
 * resources of CQL endpoints (as specified in the Clarin center registry), 
 * and search in these resources. The endpoints/resources selection is 
 * optional, by default all the endpoints root resources are selected.
 * 
 * If invoked with 'x-aggregation-context' and 'query' parameter, 
 * the aggregator will pre-select provided resources and fill in the query field.
 * This mechanism is currently used by VLO.
 * Example:
 * POST http://weblicht.sfs.uni-tuebingen.de/Aggregator HTTP/1.1
 * operation = searchRetrieve &
 * version = 1.2 &
 * query = bellen &
 * x-aggregation-context = {"http://fedora.clarin-d.uni-saarland.de/sru/":["hdl:11858/00-246C-0000-0008-5F2A-0"]}
 * 
 * 
 * Additionally, if run with the a URL query string parameter 'mode', the 
 * special behavior of the aggregator is triggered:
 * 
 * /?mode=testing
 * corresponds to the mode where the CQL endpoints are taken not from Clarin 
 * center repository, but from a hard-coded endpoints list; this functionality
 * is useful for testing the development instances of endpoints, before they
 * are moved to production. Was done to meet the request from MPI.
 * 
 * /?mode=search
 * corresponds to the mode where the aggregator page is requested with the 
 * already known query and (optionally) resources to search in, and if the
 * immediate search is desired. In this case the aggregator search results 
 * page is displayed and search results of the provided query start to fill
 * it in immediately (i.e. users don't need to click 'search' in the aggregator 
 * page). Was done to meet the request from CLARIN ERIC (Martin Wynne 
 * contacted us).
 * 
 * /?mode=live
 * corresponds to the mode where the information about corpora are taken not 
 * from the scan cache (crawled in advance), but loaded live, starting from
 * the request to center registry and then performing scan operation requests on 
 * each CQL  endpoint listed there. It takes time to get the corresponding 
 * responses from the endpoints, therefore the Aggregator page loads very slow 
 * in this mode. But this mode is useful for testing of the newly added or 
 * changed corpora without waiting for the next crawl.
 *
 * 
 * @author Yana Panchenko
 */
public class Aggregator extends SelectorComposer<Component> {

    private static final Logger LOGGER = Logger.getLogger(Aggregator.class.getName());
    @Wire
    private Textbox searchString;
    @Wire
    private Popup wspaceSigninpop;
    @Wire
    private Textbox wspaceUserName;
    @Wire
    private Textbox wspaceUserPwd;
    private int exportDataType = 1;
    @Wire
    private Div aboutDiv;
    @Wire
    private Label aboutLabel;
    @Wire
    private Div soDiv;
    private SearchOptions searchOptionsComposer;
    @Wire
    private Label soLabel;
    @Wire
    private Div srDiv;
    private SearchResults searchResultsComposer;
    @Wire
    private Label srLabel;
    @Wire
    private Div helpDiv;
    @Wire
    private Label helpLabel;
    @Wire
    private Progressmeter pMeter;
    @Wire
    private Menubar menubar;
    @Wire
    private North controls1;
    @Wire
    private South controls2;
    @Wire
    private A prevButton;
    @Wire
    private A nextButton;
    @Wire
    private Label tooltipPrevText;
    @Wire
    private Label tooltipNextText;
    @Wire
    private Menuitem weblichtTcf;
    
    private int[] searchOffset = new int[]{1, 0}; // start and size
    private ControlsVisibility controlsVisibility;
    private PagesVisibility pagesVisibility;

    private String weblichtUrl; // defined in web.xml
    public static final String MODE_PARAM = "mode";
    public static final String MODE_PARAM_VALUE_TEST = "testing";
    public static final String MODE_PARAM_VALUE_SEARCH = "search";
    public static final String MODE_PARAM_VALUE_LIVE = "live";
    
    
    
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        processContext();
        processParameters();
        searchOptionsComposer = (SearchOptions) soDiv.getChildren().get(0).getChildren().get(0).getAttribute("$" + SearchOptions.class.getSimpleName());
        searchOptionsComposer.setAggregatorController(this);
        searchResultsComposer = (SearchResults) srDiv.getChildren().get(0).getChildren().get(0).getAttribute("$" + SearchResults.class.getSimpleName());
        pagesVisibility = new PagesVisibility(aboutDiv, aboutLabel, soDiv, soLabel, srDiv, srLabel, helpDiv, helpLabel);
        controlsVisibility = new ControlsVisibility(controls1, controls2, pMeter, menubar, prevButton, nextButton);
        searchResultsComposer.setVisibilityControllers(pagesVisibility, controlsVisibility);
    }

    @Listen("onClick = #searchButton")
    public void onExecuteSearch(Event ev) {
        Map<String, Set<Corpus>> selectedCorpora = searchOptionsComposer.getSelectedCorpora();
        boolean emptyCorpora = true;
        for (Set<Corpus> corpora : selectedCorpora.values()) {
            if (!corpora.isEmpty()) {
                emptyCorpora = false;
                break;
            }
        }
        if (emptyCorpora) {
            Messagebox.show("No corpora is selected. To perform the search, please select corus/corpora of interest by checking the corpora checkboxes.", "FCS", 0, Messagebox.INFORMATION);
        } else if (searchString.getText().isEmpty()) {
            Messagebox.show("No query is specified. To perform the search, please enter a keyword of interest in the search input field, e.g. Elefant, and press the 'Search' button.", "FCS", 0, Messagebox.INFORMATION);
        } else {
            int maxRecords = searchOptionsComposer.getMaxRecords();
            String searchLang = searchOptionsComposer.getSearchLang();
            //searchOffset = new int[]{1, 0};
            searchOffset = new int[]{1, 0};
            searchOffset[0] = searchOffset[0] + searchOffset[1];
            searchOffset[1] = maxRecords;
            searchResultsComposer.executeSearch(selectedCorpora, searchOffset[0], maxRecords, searchString.getText(), searchLang);
            if (searchLang.equals(Languages.ANY_LANGUAGE_NAME)) {
                this.weblichtTcf.setVisible(false);
            } else {
                this.weblichtTcf.setVisible(true);
            }
            onClickSearchResult(null);
        }
    }

    @Listen("onOK = #searchString")
    public void onEnterSearchString(Event ev) {
        onExecuteSearch(ev);
    }

    @Listen("onClick=#clearResults")
    public void onClearResults(Event ev) {
        this.searchResultsComposer.clearResults();
    }

    @Listen("onClick=#downloadCSV")
    public void onExportResultsCSV(Event ev) {
        searchResultsComposer.exportCSV();
    }

    @Listen("onClick=#downloadTCF")
    public void onExportResultsTCF(Event ev) {
        searchResultsComposer.exportTCF();
    }
    
    @Listen("onClick=#downloadText")
    public void onExportResultsText(Event ev) {
        searchResultsComposer.exportText();
    }
    
    @Listen("onClick=#downloadExcel")
    public void onExportResultsExcel(Event ev) {
        searchResultsComposer.exportExcel();
    }

    @Listen("onClick=#exportPWCSV")
    public void onExportResultsPWCSV(Event ev) {
        exportDataType = 1;
        wspaceSigninpop.open(srDiv, "top_center");
    }

    @Listen("onClick=#exportPWTCF")
    public void onExportResultsPWTCF(Event ev) {
        exportDataType = 0;
        wspaceSigninpop.open(srDiv, "top_center");
    }
    
    @Listen("onClick=#exportPWText")
    public void onExportResultsPWText(Event ev) {
        exportDataType = 2;
        wspaceSigninpop.open(srDiv, "top_center");
    }
    
    @Listen("onClick=#exportPWExcel")
    public void onExportResultsPWExcel(Event ev) {
        exportDataType = 3;
        wspaceSigninpop.open(srDiv, "top_center");
    }
    
    @Listen("onClick=#weblichtText")
    public void onUseWebLichtOnText(Event ev) {
        String url = searchResultsComposer.useWebLichtOnText();
        if (url != null) {
            Executions.getCurrent().sendRedirect(weblichtUrl
                + url, "_blank");
        }
    }
    
    @Listen("onClick=#weblichtTcf")
    public void onUseWebLichtOnTcf(Event ev) {
        String url = searchResultsComposer.useWebLichtOnToks();
        if (url != null) {
            Executions.getCurrent().sendRedirect(weblichtUrl
                + url, "_blank");
        }
    }

    @Listen("onClick=#wspaceSigninBtn")
    public void onSignInExportResults(Event ev) {
        String user = wspaceUserName.getValue();
        String pswd = wspaceUserPwd.getValue();
        wspaceUserPwd.setValue("");
        if (user.isEmpty() || pswd.isEmpty()) {
            Messagebox.show("Need user name and password!");
        } else {
            wspaceSigninpop.close();
            if (exportDataType == 0) {
                searchResultsComposer.exportPWTCF(user, pswd);
            } else if (exportDataType == 1) {
                searchResultsComposer.exportPWCSV(user, pswd);
            } else if (exportDataType == 2) {
                searchResultsComposer.exportPWText(user, pswd);
            } else if (exportDataType == 3) {
                searchResultsComposer.exportPWExcel(user, pswd);
            }
        }
    }

    @Listen("onOK=#wspaceUserPwd")
    public void onSignInExportResultsPwdOK(Event ev) {
        onSignInExportResults(ev);
    }

    @Listen("onClick=#wspaceCancelBtn")
    public void onSignInPWCancel(Event ev) {
        wspaceUserPwd.setValue("");
        wspaceSigninpop.close();
    }

    @Listen("onClick = #helpLabel")
    public void onClickHelp(Event ev) {
        this.pagesVisibility.openHelp();
        this.controlsVisibility.disableControls1();
        this.controlsVisibility.disableControls2();
    }

    @Listen("onClick = #aboutLabel")
    public void onClickAbout(Event ev) {
        this.pagesVisibility.openAbout();
        this.controlsVisibility.disableControls1();
        this.controlsVisibility.disableControls2();
    }

    @Listen("onClick = #soLabel")
    public void onClickAdvSearch(Event ev) {
        this.pagesVisibility.openSearchOptions();
        this.controlsVisibility.disableControls1();
        this.controlsVisibility.disableControls2();
    }

    @Listen("onClick = #srLabel")
    public void onClickSearchResult(Event ev) {
        setupPrevNextSearchTooltips();
        this.pagesVisibility.openSearchResult();
        if (this.searchResultsComposer.hasSearchInProgress()) {
            this.controlsVisibility.enableControls2();
        }
        if (this.searchResultsComposer.hasResults()) {
            this.controlsVisibility.enableControls1();
            this.controlsVisibility.enableControls2();
        }
        
    }

    @Listen("onClick = #prevButton")
    public void onSearchPrev(Event ev) {
        Map<String, Set<Corpus>> selectedCorpora = searchOptionsComposer.getSelectedCorpora();
        boolean emptyCorpora = true;
        for (Set<Corpus> corpora : selectedCorpora.values()) {
            if (!corpora.isEmpty()) {
                emptyCorpora = false;
                break;
            }
        }
        if (emptyCorpora) {
            Messagebox.show("No corpora is selected. To perform the search, please select corus/corpora of interest by checking the corpora checkboxes.", "FCS", 0, Messagebox.INFORMATION);
        } else if (searchString.getText().isEmpty()) {
            Messagebox.show("No query is specified. To perform the search, please enter a keyword of interest in the search input field, e.g. Elefant, and press the 'Search' button.", "FCS", 0, Messagebox.INFORMATION);
        } else {
            int maxRecords = searchOptionsComposer.getMaxRecords();
            String searchLang = searchOptionsComposer.getSearchLang();
            //searchOffset[0] = searchOffset[0] - searchOffset[1];
            searchOffset[0] = searchOffset[0] - maxRecords;
            if (searchOffset[0] < 1) {
                searchOffset[0] = 1;
            }
            searchOffset[1] = maxRecords;
            searchResultsComposer.executeSearch(selectedCorpora, searchOffset[0], maxRecords, searchString.getText(), searchLang);
            if (searchLang.equals(Languages.ANY_LANGUAGE_NAME)) {
                this.weblichtTcf.setVisible(false);
            } else {
                this.weblichtTcf.setVisible(true);
            }
            onClickSearchResult(null);
        }
    }


    @Listen("onClick = #nextButton")
    public void onSearchNext(Event ev) {
        Map<String, Set<Corpus>> selectedCorpora = searchOptionsComposer.getSelectedCorpora();
        boolean emptyCorpora = true;
        for (Set<Corpus> corpora : selectedCorpora.values()) {
            if (!corpora.isEmpty()) {
                emptyCorpora = false;
                break;
            }
        }
        if (emptyCorpora) {
            Messagebox.show("No corpora is selected. To perform the search, please select corus/corpora of interest by checking the corpora checkboxes.", "FCS", 0, Messagebox.INFORMATION);
        } else if (searchString.getText().isEmpty()) {
            Messagebox.show("No query is specified. To perform the search, please enter a keyword of interest in the search input field, e.g. Elefant, and press the 'Search' button.", "FCS", 0, Messagebox.INFORMATION);
        } else {
            int maxRecords = searchOptionsComposer.getMaxRecords();
            String searchLang = searchOptionsComposer.getSearchLang();
            searchOffset[0] = searchOffset[0] + searchOffset[1];
            searchOffset[1] = maxRecords;
            searchResultsComposer.executeSearch(selectedCorpora, searchOffset[0], maxRecords, searchString.getText(), searchLang);
            if (searchLang.equals(Languages.ANY_LANGUAGE_NAME)) {
                this.weblichtTcf.setVisible(false);
            } else {
                this.weblichtTcf.setVisible(true);
            }
            onClickSearchResult(null);
        }
    }

    private void processParameters() {
        String[] paramValue;
        String query = null;
        paramValue = Executions.getCurrent().getParameterMap().get(SRUCQL.SEARCH_QUERY_PARAMETER);
        if (paramValue != null) {
            query = paramValue[0].trim();
            searchString.setValue(query);
        }
        LOGGER.log(Level.INFO, "Received parameter: query[{0}], ", query);
        paramValue = Executions.getCurrent().getParameterMap().get(SRUCQL.OPERATION);
        String operationString = null;
        if (paramValue != null) {
            operationString = paramValue[0].trim();
            if (!operationString.equals(SRUCQL.SEARCH_RETRIEVE)) {
                Messagebox.show("Not supported operation " + operationString, "FCS", 0, Messagebox.INFORMATION);
            }
        }
        LOGGER.log(Level.INFO, "Received parameter: operation[{0}], ", operationString);
    }

    private void setupPrevNextSearchTooltips() {
        int startHit = searchOffset[0] - searchOptionsComposer.getMaxRecords();
        if (startHit < 1) {
            startHit = 1;
        }
        int endHit = searchOffset[0] - 1;
        tooltipPrevText.setValue("hits " + 
                    startHit + "-" + endHit);
        startHit = searchOffset[0] + searchOffset[1];
        endHit = startHit + searchOptionsComposer.getMaxRecords() - 1;
        tooltipNextText.setValue("hits " + 
                    startHit + "-" + endHit);
    }

    private void processContext() {
        InitialContext context;
        try {
            context = new InitialContext();
            weblichtUrl = (String) context.lookup("java:comp/env/weblicht-url");
        } catch (NamingException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

}
