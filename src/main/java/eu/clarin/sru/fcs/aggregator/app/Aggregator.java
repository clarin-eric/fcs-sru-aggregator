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
import org.zkoss.zul.A;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Menubar;
import org.zkoss.zul.Menuitem;
import org.zkoss.zul.North;
import org.zkoss.zul.Popup;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.South;

/**
 * Main window of the Aggregator application.
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
    Progressmeter pMeter;
    @Wire
    Menubar menubar;
    @Wire
    North controls1;
    @Wire
    South controls2;
    @Wire
    A prevButton;
    @Wire
    A nextButton;
    
    @Wire
    Menuitem weblichtTcf;
    
    private int[] searchOffset = new int[]{1, 0}; // start and size
    private ControlsVisibility controlsVisibility;
    private PagesVisibility pagesVisibility;

    private static final String WEBLICHT_URL = "https://weblicht.sfs.uni-tuebingen.de/WebLicht-4/?input=";
    
    
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        processParameters();
        searchOptionsComposer = (SearchOptions) soDiv.getChildren().get(0).getChildren().get(0).getAttribute("$" + SearchOptions.class.getSimpleName());
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
            searchOffset = new int[]{1, 0};
            searchResultsComposer.executeSearch(selectedCorpora, maxRecords, searchString.getText(), searchOffset, searchLang);
            if (searchLang.equals("anylang")) {
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
            Executions.getCurrent().sendRedirect(WEBLICHT_URL
                + url, "_blank");
        } else {
            Messagebox.show("Sorry, drop-off/WebLicht error!");
        }
    }
    
    @Listen("onClick=#weblichtTcf")
    public void onUseWebLichtOnTcf(Event ev) {
        String url = searchResultsComposer.useWebLichtOnToks();
        if (url != null) {
            Executions.getCurrent().sendRedirect(WEBLICHT_URL
                + url, "_blank");
        } else {
            Messagebox.show("Sorry, drop-off/WebLicht error!");
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
            searchOffset[0] = searchOffset[0] - 2 * searchOffset[1];
            if (searchOffset[0] < 1) {
                searchOffset[0] = 1;
                searchOffset[1] = 0;
            }
            searchResultsComposer.executeSearch(selectedCorpora, maxRecords, searchString.getText(), searchOffset, searchLang);
            if (searchLang.equals("anylang")) {
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
            searchResultsComposer.executeSearch(selectedCorpora, maxRecords, searchString.getText(), searchOffset, searchLang);
            if (searchLang.equals("anylang")) {
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
        paramValue = Executions.getCurrent().getParameterMap().get("query");
        if (paramValue != null) {
            query = paramValue[0].trim();
            searchString.setValue(query);
        }
        LOGGER.log(Level.INFO, "Received parameter: query[{0}], ", query);
        paramValue = Executions.getCurrent().getParameterMap().get("operation");
        String operationString = null;
        if (paramValue != null) {
            operationString = paramValue[0].trim();
            if (!operationString.equals("searchRetrieve")) {
                Messagebox.show("Not supported operation " + operationString, "FCS", 0, Messagebox.INFORMATION);
            }
        }
        LOGGER.log(Level.INFO, "Received parameter: operation[{0}], ", operationString);
    }
}
