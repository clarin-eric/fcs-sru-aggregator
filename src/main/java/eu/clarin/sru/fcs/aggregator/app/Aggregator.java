package eu.clarin.sru.fcs.aggregator.app;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.jersey.api.client.Client;
import eu.clarin.sru.client.SRUVersion;
import eu.clarin.sru.fcs.aggregator.data.CenterRegistry;
import eu.clarin.sru.fcs.aggregator.sparam.CorpusTreeModel;
import eu.clarin.sru.fcs.aggregator.sparam.CorpusTreeNodeRenderer;
import eu.clarin.sru.fcs.aggregator.sresult.SearchResultsController;
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
import eu.clarin.sru.client.fcs.DataViewKWIC;
import eu.clarin.sru.fcs.aggregator.data.SearchResult;
import eu.clarin.sru.fcs.aggregator.sparam2.Corpus2;
import eu.clarin.sru.fcs.aggregator.sparam2.Corpus2Renderer;
import eu.clarin.sru.fcs.aggregator.sparam2.CorpusTreeModel2;
import eu.clarin.weblicht.wlfxb.tc.api.GeoLongLatFormat;
import eu.clarin.weblicht.wlfxb.tc.api.Token;
import eu.clarin.weblicht.wlfxb.tc.xb.TextCorpusStored;
import eu.clarin.weblicht.wlfxb.xb.WLData;
import javax.ws.rs.core.MediaType;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.DefaultTreeNode;
import org.zkoss.zul.Div;
import org.zkoss.zul.Menubar;
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

    private static final Logger logger = Logger.getLogger(Aggregator.class.getName());
    
    @Wire
    private Textbox searchString;
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
    //private SearchResultsController searchResultsController;
    private CenterRegistry registry;
    private boolean testingMode = false;
    
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
    Button prevButton;
    @Wire
    Button nextButton;
    
    
    
    private ControlsVisibility controlsVisibility;

    @Override
    public void doAfterCompose(Component comp) throws Exception {

        super.doAfterCompose(comp);

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
        
        
        searchOptionsComposer = (SearchOptions) soDiv.getChildren().get(0).getChildren().get(0).getAttribute("$" + SearchOptions.class.getSimpleName());
        searchResultsComposer = (SearchResults) srDiv.getChildren().get(0).getChildren().get(0).getAttribute("$" + SearchResults.class.getSimpleName()); 
        
        controlsVisibility = new ControlsVisibility(controls1, controls2, pMeter, menubar, prevButton, nextButton);
        
    }


    @Listen("onClick = #searchButton")
    public void onExecuteSearch(Event ev) {
        //searchResultsController.executeSearch(tree.getSelectedItems(), maxRecords, searchString.getText(), version);
        Map<String,Set<Corpus2>> selectedCorpora = searchOptionsComposer.getSelectedCorpora();
        boolean emptyCorpora = true;
        for (Set<Corpus2> corpora : selectedCorpora.values()) {
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
            searchResultsComposer.executeSearch(selectedCorpora, maxRecords, searchString.getText(), controlsVisibility);
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
        //searchResultsController.exportCSV();
    }

    @Listen("onClick=#downloadTCF")
    public void onExportResultsTCF(Event ev) {
        //searchResultsController.exportTCF();
        searchResultsComposer.exportTCF();
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
            } else {
                searchResultsComposer.exportPWCSV(user, pswd);
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

    private void tempMap() {
        ClientConfig config = new DefaultClientConfig();
        Client client = Client.create(config);
        mapGenerator = client.resource(MAPS_SERVICE_URL);
        TextCorpusStored tc = new TextCorpusStored("en");
        Token t1 = tc.createTokensLayer().addToken("Virginia");
        List<Token> s1 = new ArrayList<Token>();
        s1.add(t1);
        tc.createSentencesLayer().addSentence(s1);
        tc.createGeoLayer("unknown", GeoLongLatFormat.DegDec);
        //tc.getGeoLayer().addPoint("138.56027", "-34.6663", 15.0, null, null, null, t1);
        WLData data = new WLData(tc);

        Iframe resultsPic = (Iframe) srDiv.getFellow("resultsPic");

        try {
            String output = mapGenerator.path("3").accept(MediaType.TEXT_HTML).type("text/tcf+xml").post(String.class, data);
            Media media = new AMedia("map-" + 4 + ".html", null, "text/html", output);
            resultsPic.setContent(media);
        } catch (Exception e) {
            Logger.getLogger(Aggregator.class.getName()).log(Level.SEVERE, "ERROR accessing the maps generator service", e);
            Messagebox.show("ERROR accessing the maps generator service \n" + e.getMessage(), "FCS", 0, Messagebox.INFORMATION);
        }
    }


    @Listen("onClick = #helpLabel")
    public void onClickHelp(Event ev) {
        this.helpDiv.setVisible(true);
        this.helpLabel.setSclass("internalLinkSelected");
        this.aboutDiv.setVisible(false);
        this.aboutLabel.setSclass("internalLink");
        this.soDiv.setVisible(false);
        this.soLabel.setSclass("internalLink");
        this.srDiv.setVisible(false);
        this.srLabel.setSclass("internalLink");
        
        this.controlsVisibility.disableControls1();
    }
    
    @Listen("onClick = #aboutLabel")
    public void onClickAbout(Event ev) {
        this.aboutDiv.setVisible(true);
        this.aboutLabel.setSclass("internalLinkSelected");
        this.helpDiv.setVisible(false);
        this.helpLabel.setSclass("internalLink");
        this.soDiv.setVisible(false);
        this.soLabel.setSclass("internalLink");
        this.srDiv.setVisible(false);
        this.srLabel.setSclass("internalLink");
        
        this.controlsVisibility.disableControls1();
    }
    
    @Listen("onClick = #soLabel")
    public void onClickAdvSearch(Event ev) {
        this.soDiv.setVisible(true);
        this.soLabel.setSclass("internalLinkSelected");
        this.aboutDiv.setVisible(false);
        this.aboutLabel.setSclass("internalLink");
        this.helpDiv.setVisible(false);
        this.helpLabel.setSclass("internalLink");
        this.srDiv.setVisible(false);
        this.srLabel.setSclass("internalLink");
        
        this.controlsVisibility.disableControls1();
    }
    
    @Listen("onClick = #srLabel")
    public void onClickSearchResult(Event ev) {
        this.srDiv.setVisible(true);
        this.srLabel.setSclass("internalLinkSelected");
        this.aboutDiv.setVisible(false);
        this.aboutLabel.setSclass("internalLink");
        this.soDiv.setVisible(false);
        this.soLabel.setSclass("internalLink");
        this.helpDiv.setVisible(false);
        this.helpLabel.setSclass("internalLink");
        
        if (this.searchResultsComposer.hasResults()) {
            this.controlsVisibility.enableControls1();
        }
    }
    
    
}
