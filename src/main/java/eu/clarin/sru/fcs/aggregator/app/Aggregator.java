package eu.clarin.sru.fcs.aggregator.app;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
    
    private Map<String, List<String>> xAggregationContext;
    private SRUVersion version = SRUVersion.VERSION_1_2;
    private SearchResultsController searchResultsController;
    private CenterRegistry registry;

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
        registry.loadChildren();
        CorpusTreeModel corporaModel = new CorpusTreeModel(registry);
        tree.setModel(corporaModel);
        tree.setItemRenderer(new CorpusTreeNodeRenderer());
        tree.setMultiple(true);

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
    
    @Listen("onClick=#addForeignEndpoint")
    public void onAddForeignEndpoint(Event ev) {
            registry.addForeignPoint(foreignEndpointSelect.getValue().split(";")[1], foreignEndpointSelect.getValue().split(";")[0]);
    }

    private void processParameters() {
        
        String[] paramValue;
        String contextJson = null;

        paramValue = Executions.getCurrent().getParameterMap().get("query");
        if (paramValue != null) {
            searchString.setValue(paramValue[0].trim());
            logger.log(Level.INFO, "Received parameter: query[{0}]",  searchString.getValue());
        }
        paramValue = Executions.getCurrent().getParameterMap().get("operation");
        if (paramValue != null) {
            String operationString = paramValue[0].trim();
            logger.log(Level.INFO, "Received parameter: operation[{0}]", operationString);
            if (!operationString.equals("searchRetrieve")) {
                logger.log(Level.SEVERE, "Not supported operation: {0}", operationString);
                Messagebox.show("CLARIN-D Federated Content Search Aggregator\n\nVersion 0.0.1", "FCS", 0, Messagebox.INFORMATION);
            }
        }
        paramValue = Executions.getCurrent().getParameterMap().get("version");
        if (paramValue != null) {
            String versionString = paramValue[0].trim();
            logger.log(Level.INFO, "Received parameter: version[{0}]", versionString);
            if (versionString.equals("1.2")) {
                version = SRUVersion.VERSION_1_2;
            } else if (versionString.equals("1.1")) {
                version = SRUVersion.VERSION_1_1;
            } else {
                logger.log(Level.SEVERE, "Not supported SRU version: {0}", versionString);
                Messagebox.show("SRU Version " + version + " not supported", "FCS", 0, Messagebox.INFORMATION);
            }
        }
        paramValue = Executions.getCurrent().getParameterMap().get("x-aggregation-context");
        if (paramValue != null) {
            contextJson = paramValue[0].trim();
            logger.log(Level.INFO, "Received parameter: x-aggregation-context[{0}]", contextJson);
        }

        if (contextJson != null) {
            Gson gson = new Gson();
            Type mapType = new TypeToken<LinkedHashMap<String, ArrayList<String>>>() {
            }.getType();
            try {
            this.xAggregationContext = gson.fromJson(contextJson, mapType);
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Error parsing JSON from x-aggregation-context:\n {0}\n {1}", new String[]{ex.getMessage(), contextJson});
                Messagebox.show("Error in x-aggregation-context parameter", "FCS", 0, Messagebox.INFORMATION);
            }
        }

    }
}
