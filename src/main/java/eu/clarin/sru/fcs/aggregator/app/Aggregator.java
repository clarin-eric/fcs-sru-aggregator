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
import eu.clarin.weblicht.wlfxb.tc.api.GeoLongLatFormat;
import eu.clarin.weblicht.wlfxb.tc.api.Token;
import eu.clarin.weblicht.wlfxb.tc.xb.TextCorpusStored;
import eu.clarin.weblicht.wlfxb.xb.WLData;
import javax.ws.rs.core.MediaType;
import org.zkoss.zul.Popup;

/**
 * Main window of the Aggregator application.
 *
 * @author Yana Panchenko
 */
public class Aggregator extends SelectorComposer<Component> {

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
    private SearchResultsController searchResultsController;
    private CenterRegistry registry;
    private boolean testingMode = false;

    @Override
    public void doAfterCompose(Component comp) throws Exception {

        super.doAfterCompose(comp);

        processParameters();

        languageSelect.setSelectedItem(anyLanguage);

        searchResultsController = new SearchResultsController(resultsBox, searchResultsProgress);
        // assign the search controller to desktop, so that it can be accessed to be shutdown when the desktop is destroyed
        Executions.getCurrent().getDesktop().setAttribute(searchResultsController.getClass().getSimpleName(), searchResultsController);
        // also add it to the list of actice controllers of the web application, so that they can be shutdown when the application stops
        Set<SearchResultsController> activeControllers = (Set<SearchResultsController>) Executions.getCurrent().getDesktop().getWebApp().getAttribute(WebAppListener.ACTIVE_SEARCH_CONTROLLERS);
        activeControllers.add(searchResultsController);

        registry = new CenterRegistry();
        registry.loadChildren(testingMode);
        CorpusTreeModel corporaModel = new CorpusTreeModel(registry);
        tree.setModel(corporaModel);
        tree.setItemRenderer(new CorpusTreeNodeRenderer());
        tree.setMultiple(true);


        //tempMap();

    }

    @Listen("onSelect = #languageSelect")
    public void onSelectLanguage(Event ev) {
        //TODO
    }

    @Listen(ZulEvents.ON_AFTER_RENDER + "=#tree")
    public void onAfterRenderCorporaTree(Event ev) {
        CorpusTreeNodeRenderer.selectEndpoints(this.tree, this.xAggregationContext);
    }

    @Listen("onClick = #selectAll")
    public void onSelectAll(Event ev) {
        Treechildren openTreeItems = tree.getTreechildren();
        for (Treeitem openItem : openTreeItems.getItems()) {
            CorpusTreeNodeRenderer.selectItem(openItem);
        }
    }

    @Listen("onClick = #deselectAll")
    public void onDeselectAll(Event ev) {
        Treechildren openTreeItems = tree.getTreechildren();
        for (Treeitem openItem : openTreeItems.getItems()) {
            CorpusTreeNodeRenderer.unselectItem(openItem);
        }
    }

    @Listen("onClick = #searchButton")
    public void onExecuteSearch(Event ev) {
        int maxRecords = Integer.parseInt(maximumRecordsSelect.getValue());
        searchResultsController.executeSearch(tree.getSelectedItems(), maxRecords, searchString.getText(), version);
    }

    @Listen("onOK = #searchString")
    public void onEnterSearchString(Event ev) {
        onExecuteSearch(ev);
    }

    @Listen("onClick=#clearResults")
    public void onClearResults(Event ev) {
        resultsBox.getChildren().clear();
    }

    @Listen("onClick=#showHelp")
    public void onShowHelp(Event ev) {
        resultsBox.getChildren().clear();
        Iframe help = new Iframe();
        help.setWidth("100%");
        help.setHeight("100%");
        help.setSrc("help.html");
        resultsBox.appendChild(help);
    }

    @Listen("onClick=#showAbout")
    public void onShowAbout(Event ev) {
        Messagebox.show("CLARIN-D Federated Content Search Aggregator\n\nVersion 0.0.1", "FCS", 0, Messagebox.INFORMATION);

    }

    @Listen("onClick=#exportResultsCSV")
    public void onExportResultsCSV(Event ev) {
        searchResultsController.exportCSV();
    }

    @Listen("onClick=#exportResultsTCF")
    public void onExportResultsTCF(Event ev) {
        searchResultsController.exportTCF();
    }

    @Listen("onClick=#exportResultsPWTCF")
    public void onExportResultsPWTCF(Event ev) {
        wspaceSigninpop.open(resultsBox, "top_center");
    }

    @Listen("onClick=#wspaceSigninBtn")
    public void onSignInExportResultsPWTCF(Event ev) {
        String user = wspaceUserName.getValue();
        String pswd = wspaceUserPwd.getValue();
        if (user.isEmpty() || pswd.isEmpty()) {
            Messagebox.show("Need user name and password!");
        } else {
            wspaceUserPwd.setValue("");
            wspaceSigninpop.close();
            searchResultsController.exportPWTCF(user, pswd);
        }
    }
    
    @Listen("onOK=#wspaceUserPwd")
    public void onSignInExportResultsPWTCFPwdOK(Event ev) {
        onSignInExportResultsPWTCF(ev);
    }
    
    @Listen("onClick=#wspaceCancelBtn")
    public void onSignInPWCancel(Event ev) {
        wspaceUserPwd.setValue("");
        wspaceSigninpop.close();
    }


    @Listen("onClick=#addForeignEndpoint")
    public void onAddForeignEndpoint(Event ev) {
        registry.addForeignPoint(foreignEndpointSelect.getValue().split(";")[1], foreignEndpointSelect.getValue().split(";")[0]);
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

        Iframe resultsPic = (Iframe) resultsBox.getFellow("resultsPic");

        try {
            String output = mapGenerator.path("3").accept(MediaType.TEXT_HTML).type("text/tcf+xml").post(String.class, data);
            Media media = new AMedia("map-" + 4 + ".html", null, "text/html", output);
            resultsPic.setContent(media);
        } catch (Exception e) {
            Logger.getLogger(Aggregator.class.getName()).log(Level.SEVERE, "ERROR accessing the maps generator service", e);
            Messagebox.show("ERROR accessing the maps generator service \n" + e.getMessage(), "FCS", 0, Messagebox.INFORMATION);
        }
    }
}
