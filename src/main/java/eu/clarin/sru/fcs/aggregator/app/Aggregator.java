package eu.clarin.sru.fcs.aggregator.app;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUExplainRequest;
import eu.clarin.sru.client.SRUExplainResponse;
import eu.clarin.sru.client.SRURecord;
import eu.clarin.sru.client.SRUSearchRetrieveRequest;
import eu.clarin.sru.client.SRUSearchRetrieveResponse;
import eu.clarin.sru.client.SRUSurrogateRecordData;
import eu.clarin.sru.client.SRUThreadedClient;
import eu.clarin.sru.client.SRUVersion;
import eu.clarin.sru.client.fcs.ClarinFCSRecordData;
import eu.clarin.sru.client.fcs.ClarinFCSRecordParser;
import eu.clarin.sru.client.fcs.DataView;
import eu.clarin.sru.client.fcs.DataViewKWIC;
import eu.clarin.sru.client.fcs.Resource;
import eu.clarin.sru.fcs.aggregator.data.CenterRegistry;
import eu.clarin.sru.fcs.aggregator.sparam.CorpusTreeModel;
import eu.clarin.sru.fcs.aggregator.sparam.CorpusTreeNodeRenderer;
import eu.clarin.sru.fcs.aggregator.sresult.SearchResultsController;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.*;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Auxhead;
import org.zkoss.zul.Auxheader;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Separator;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treechildren;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;
import org.zkoss.zul.event.ZulEvents;

/**
 * Main window of the Aggregator application.
 * 
 * @author Yana Panchenko
 */
public class Aggregator extends SelectorComposer<Component> {

    private static final Logger logger = Logger.getLogger("FCS-AGGREGATOR");
//    @Wire
//    private Grid anzeigeGrid;
    @Wire
    private Textbox searchString;
    @Wire
    private Combobox languageSelect;
    @Wire
    private Button searchButton;
    @Wire
    private Groupbox allCorpora;
//    @Wire
//    private Comboitem german;
    @Wire
    private Comboitem anyLanguage;
    @Wire
    private Window resultsBox;
    @Wire
    private Button selectAll;
    @Wire
    private Button deselectAll;
    @Wire
    private Window mainWindow;
    @Wire
    private Combobox maximumRecordsSelect;
    @Wire
    private Button addForeignEndpoint;
    @Wire
    Combobox foreignEndpointSelect;
    @Wire
    private Tree tree;
    private Map<String, List<String>> xAggregationContext;
    private SearchResultsController searchResultsController;
    private CenterRegistry registry;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        processParameters();

        languageSelect.setSelectedItem(anyLanguage);
        searchResultsController = new SearchResultsController(resultsBox);
        // assign it to desktop, so that it can be accessed to be shutdown when the desktop is destroyed
        // TODO the registry/tree also has to be shutdown properly???
        Executions.getCurrent().getDesktop().setAttribute(searchResultsController.getClass().getSimpleName(), searchResultsController);
        
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
        logger.info("Executing Search.");
        searchResultsController.executeSearch(tree.getSelectedItems(), maxRecords, searchString.getText());
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

        int i, i2, i3;
        String temp = "";
        boolean somethingToExport = false;

        for (i = 0; i < resultsBox.getChildren().size(); i++) {
            if (resultsBox.getChildren().get(i) instanceof Grid) {
                somethingToExport = true;
                Grid aGrid = (Grid) resultsBox.getChildren().get(i);
                Rows rows = aGrid.getRows();

                for (i2 = 0; i2 < rows.getChildren().size(); i2++) {
                    Row r = (Row) rows.getChildren().get(i2);

                    for (i3 = 0; i3 < r.getChildren().size(); i3++) {
                        Label l = (Label) r.getChildren().get(i3);
                        temp = temp + "\"" + l.getValue().replace("\"", "QUOTE") + "\"";
                        if (i3 < r.getChildren().size() - 1) {
                            temp = temp + ",";
                        } //if i3
                    } //for i3
                    temp = temp + "\n";
                } // for i2
            } // if grid

        } // for i ...

        if (somethingToExport) {

            Filedownload.save(temp, "text/plain", "ClarinDFederatedContentSearch.csv");
        } else {
            Messagebox.show("Nothing to export!");
        }
    }

    @Listen("onClick=#exportResultsTCF")
    public void onExportResultsTCF(Event ev) {

        int i, i2, i3;
        String temp = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><D-Spin xmlns=\"http://www.dspin.de/data\" version=\"0.4\">\n<MetaData xmlns=\"http://www.dspin.de/data/metadata\">\n";
        temp = temp + "<source>CLARIN-D Federated Content Search</source>\n</MetaData>\n  <TextCorpus xmlns=\"http://www.dspin.de/data/textcorpus\" lang=\"de\">\n<text>";


        boolean somethingToExport = false;

        for (i = 0; i < resultsBox.getChildren().size(); i++) {
            if (resultsBox.getChildren().get(i) instanceof Grid) {
                somethingToExport = true;
                Grid aGrid = (Grid) resultsBox.getChildren().get(i);
                Rows rows = aGrid.getRows();

                for (i2 = 0; i2 < rows.getChildren().size(); i2++) {
                    Row r = (Row) rows.getChildren().get(i2);

                    for (i3 = 0; i3 < r.getChildren().size(); i3++) {
                        Label l = (Label) r.getChildren().get(i3);
                        temp = temp + l.getValue() + " ";
                    } //for i3
                    temp = temp + "\n";
                } // for i2
            } // if grid

        } // for i ...

        if (somethingToExport) {
            temp = temp + "</text>\n</TextCorpus>\n</D-Spin>";
            Filedownload.save(temp, "text/tcf+xml", "ClarinDFederatedContentSearch.xml");
        } else {
            Messagebox.show("Nothing to export!");
        }
    }
    
    @Listen("onClick=#addForeignEndpoint")
    public void onAddForeignEndpoint(Event ev) {
            registry.addForeignPoint(foreignEndpointSelect.getValue().split(";")[1], foreignEndpointSelect.getValue().split(";")[0]);
    }

    private void processParameters() {
        String[] paramValue;
        String contextJson = null;

        //TODO use them???
        String operationString = null;
        String versionString = null;

        paramValue = Executions.getCurrent().getParameterMap().get("query");
        if (paramValue != null) {
            searchString.setValue(paramValue[0]);
        }
        paramValue = Executions.getCurrent().getParameterMap().get("operation");
        if (paramValue != null) {
            operationString = paramValue[0];
            if (!operationString.equals("searchRetrieve")) {
                logger.severe("Not supported operation: " + operationString);
            }
        }
        paramValue = Executions.getCurrent().getParameterMap().get("version");
        if (paramValue != null) {
            versionString = paramValue[0];
        }
        paramValue = Executions.getCurrent().getParameterMap().get("x-aggregation-context");
        if (paramValue != null) {
            contextJson = paramValue[0];
        }

        logger.info("query: " + searchString.getValue());
        logger.info("operation: " + operationString);
        logger.info("version: " + versionString);
        logger.info("x-aggregation-context: " + contextJson);

        if (contextJson != null) {
            Gson gson = new Gson();
            Type mapType = new TypeToken<LinkedHashMap<String, ArrayList<String>>>() {
            }.getType();
            this.xAggregationContext = gson.fromJson(contextJson, mapType);
            //System.out.println("selectedEndpoints: " + selectedEndpoints);
            //selectEndpoints(selectedEndpoints);
        }


    }
}
