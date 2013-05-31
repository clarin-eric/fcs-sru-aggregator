package eu.clarin.sru.fcs.aggregator.app;

import eu.clarin.sru.client.SRUVersion;
import eu.clarin.sru.fcs.aggregator.data.CenterRegistry;
import eu.clarin.sru.fcs.aggregator.data.Institution;
import eu.clarin.sru.fcs.aggregator.data.SearchResult;
import eu.clarin.sru.fcs.aggregator.sparam.CorpusTreeNodeRenderer;
import eu.clarin.sru.fcs.aggregator.sparam2.Corpus2;
import eu.clarin.sru.fcs.aggregator.sparam2.Corpus2Renderer;
import eu.clarin.sru.fcs.aggregator.sparam2.CorpusByInstitutionComparator;
import eu.clarin.sru.fcs.aggregator.sparam2.CorpusByInstitutionDComparator;
import eu.clarin.sru.fcs.aggregator.sparam2.CorpusByNameComparator;
import eu.clarin.sru.fcs.aggregator.sparam2.CorpusByNameDComparator;
import eu.clarin.sru.fcs.aggregator.sparam2.CorpusTreeModel2;
import eu.clarin.sru.fcs.aggregator.sparam2.Languages;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
import org.zkoss.zul.DefaultTreeNode;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treechildren;
import org.zkoss.zul.Treecol;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.event.ZulEvents;

/**
 *
 * @author Yana Panchenko
 */
public class SearchOptions extends SelectorComposer<Component> {

    private static final Logger logger = Logger.getLogger(Aggregator.class.getName());
    @Wire
    private Combobox languageSelect;
    @Wire
    private Comboitem anyLanguage;
    @Wire
    private Combobox maximumRecordsSelect;
    @Wire
    private Tree tree;
    private Map<String, List<String>> xAggregationContext;
    private CenterRegistry registry;
    private boolean testingMode = false;
    private Corpus2Renderer corpusRenderer;
    private Languages languages;
    @Wire
    private Groupbox allCorpora;
    

    //private Map<String,List<Corpus2>> corporaByLanguage = new HashMap<String,List<Corpus2>>();
    @Override
    public void doAfterCompose(Component comp) throws Exception {

        super.doAfterCompose(comp);

        languages = (Languages) Executions.getCurrent().getDesktop().getWebApp().getAttribute(WebAppListener.LANGUAGES);
        String[] paramValue = Executions.getCurrent().getParameterMap().get("mode");
        if (paramValue != null) {
            String mode = paramValue[0].trim();
            if (mode.equals("testing")) {
                this.testingMode = true;
            }
        } 
        
        registry = new CenterRegistry();
        if (testingMode) {
            registry.loadChildren(testingMode);
        }
        Corpus2 rootCorpus = new Corpus2();
        List<DefaultTreeNode<Corpus2>> rootChildren = Corpus2.initCorpusChildren(registry);
        DefaultTreeNode<Corpus2> root = new DefaultTreeNode(rootCorpus, rootChildren);
        CorpusTreeModel2 corporaModel = new CorpusTreeModel2(root);

        corporaModel.setMultiple(true);
        tree.setModel(corporaModel);
        corpusRenderer = new Corpus2Renderer();
        tree.setItemRenderer(corpusRenderer);
        Treecol nameCol = (Treecol) tree.getTreecols().getFellow("nameCol");
        nameCol.setSortAscending(new CorpusByNameComparator());
        nameCol.setSortDescending(new CorpusByNameDComparator());
        Treecol instCol = (Treecol) tree.getTreecols().getFellow("instCol");
        instCol.setSortAscending(new CorpusByInstitutionComparator());
        instCol.setSortDescending(new CorpusByInstitutionDComparator());
        //tree.setSizedByContent(true);

        languageSelect.setSelectedItem(anyLanguage);

    }

    @Listen("onOpen=#allCorpora")
    public void onOpenCorpora(Event ev) {
        if (allCorpora.isOpen()) {
            allCorpora.setWidth("100%");
        } else {
            allCorpora.setWidth("600px");
        }
    }

    @Listen("onSelect = #languageSelect")
    public void onSelectLanguage(Event ev) {
        Combobox cbox = (Combobox) ev.getTarget();
        //System.out.println(cbox.getSelectedItem().getValue());
        String selectedLang = cbox.getSelectedItem().getValue();
        //if (selectedLang.equals("anylang")) {
        // do nothing
        //} else {
        //List<Treeitem> itemsToRemove = new ArrayList<Treeitem>();
        for (Component comp : tree.getTreechildren().getChildren()) {
            Treeitem treeitem = (Treeitem) comp;
            DefaultTreeNode<Corpus2> node = (DefaultTreeNode<Corpus2>) treeitem.getValue();
            Corpus2 corpus = node.getData();
            if (corpus.getLanguages().contains(selectedLang) || selectedLang.equals("anylang")) {
                treeitem.setVisible(true);
            } else {
                corpusRenderer.updateItem(treeitem, false);
                treeitem.setVisible(false);
                //itemsToRemove.add(treeitem);
            }
        }
//            for (Treeitem item : itemsToRemove) {
//                //item.detach();
//                //parentItem.getChildren().remove(item);
//                
//            }
        //}
    }

    @Listen(ZulEvents.ON_AFTER_RENDER + "=#tree")
    public void onAfterRenderCorporaTree(Event ev) {
        onSelectAll(null);
        loadLanguages();
    }

    @Listen("onClick = #selectAll")
    public void onSelectAll(Event ev) {
        Treechildren openTreeItems = tree.getTreechildren();
        for (Treeitem openItem : openTreeItems.getItems()) {
            corpusRenderer.updateItem(openItem, true);
        }
    }

    @Listen("onClick = #deselectAll")
    public void onDeselectAll(Event ev) {
        Treechildren openTreeItems = tree.getTreechildren();
        for (Treeitem openItem : openTreeItems.getItems()) {
            corpusRenderer.updateItem(openItem, false);
        }
    }

    void selectCorpora(Map<String, List<String>> xAggregationContext) {
        onDeselectAll(null);
        Treechildren openTreeItems = tree.getTreechildren();
        for (Treeitem openItem : openTreeItems.getItems()) {
            DefaultTreeNode<Corpus2> node = (DefaultTreeNode<Corpus2>) openItem.getValue();
            Corpus2 data = node.getData();
            List<String> handles = xAggregationContext.get(data.getEndpointUrl());
            if (handles == null) {
                if (data.getEndpointUrl().endsWith("/")) {
                     handles =  xAggregationContext.get(
                             data.getEndpointUrl().substring(0, data.getEndpointUrl().length() - 1));
                } else {
                    handles =  xAggregationContext.get(
                             data.getEndpointUrl() + "/");
                }
            }
            if (handles != null) {
                if (handles.isEmpty()) {
                    corpusRenderer.updateItem(openItem, true);
                } else {
                    for (String handle : handles) {
                        if (handle.equals(data.getHandle())) {
                            corpusRenderer.updateItem(openItem, true);
                        }
                    }
                }
            }
        }
    }

    public int getMaxRecords() {
        return Integer.parseInt(maximumRecordsSelect.getValue());
    }

    public Map<String, Set<Corpus2>> getSelectedCorpora() {
        return corpusRenderer.getSelectedCorpora();
    }

//    private void processParameters() {
//
//        String[] paramValue;
//        String contextJson = null;
//
//        String[] paramsReceived = new String[4];
//
//        paramValue = Executions.getCurrent().getParameterMap().get("query");
//        if (paramValue != null) {
//            searchString.setValue(paramValue[0].trim());
//            paramsReceived[0] = searchString.getValue();
//        }
//        paramValue = Executions.getCurrent().getParameterMap().get("operation");
//        if (paramValue != null) {
//            String operationString = paramValue[0].trim();
//            paramsReceived[1] = operationString;
//            if (!operationString.equals("searchRetrieve")) {
//                Messagebox.show("Not supported operation " + operationString, "FCS", 0, Messagebox.INFORMATION);
//            }
//        }
//        paramValue = Executions.getCurrent().getParameterMap().get("version");
//        if (paramValue != null) {
//            String versionString = paramValue[0].trim();
//            paramsReceived[2] = versionString;
//            if (versionString.equals("1.2")) {
//                version = SRUVersion.VERSION_1_2;
//            } else if (versionString.equals("1.1")) {
//                version = SRUVersion.VERSION_1_1;
//            } else {
//                Messagebox.show("SRU Version " + version + " not supported", "FCS", 0, Messagebox.INFORMATION);
//            }
//        }
//        paramValue = Executions.getCurrent().getParameterMap().get("x-aggregation-context");
//        if (paramValue != null) {
//            contextJson = paramValue[0].trim();
//            paramsReceived[3] = contextJson;
//        }
//        logger.log(Level.INFO, "Received parameters: query[{0}], operation[{1}], version[{2}], x-aggregation-context[{3}], ", paramsReceived);
//
//        paramValue = Executions.getCurrent().getParameterMap().get("mode");
//        if (paramValue != null) {
//            String mode = paramValue[0].trim();
//            if (mode.equals("testing")) {
//                testingMode = true;
//            }
//        }
//
//        if (contextJson != null) {
//            Gson gson = new Gson();
//            Type mapType = new TypeToken<LinkedHashMap<String, ArrayList<String>>>() {
//            }.getType();
//            try {
//                this.xAggregationContext = gson.fromJson(contextJson, mapType);
//            } catch (Exception ex) {
//                logger.log(Level.SEVERE, "Error parsing JSON from x-aggregation-context: {0} {1}", new String[]{ex.getMessage(), contextJson});
//                Messagebox.show("Error in x-aggregation-context parameter", "FCS", 0, Messagebox.INFORMATION);
//            }
//        }
//
//    }
    private void loadLanguages() {

        Set<String> availableLangs = new HashSet<String>();
//        for (Set<Corpus2> corpora : corpusRenderer.getSelectedCorpora().values()) {
//            for (Corpus2 corpus : corpora) {
//                availableLangs.addAll(corpus.getLanguages());
//            }
//        }

        Treechildren treeItems = tree.getTreechildren();
        for (Treeitem item : treeItems.getItems()) {
            DefaultTreeNode<Corpus2> node = item.getValue();
            Corpus2 corpus = node.getData();
            for (String langCode : corpus.getLanguages()) {
                availableLangs.add(langCode);
//                if (!this.corporaByLanguage.containsKey(langCode)) {
//                    this.corporaByLanguage.put(langCode, new ArrayList<Corpus2>());
//                }
//                this.corporaByLanguage.get(langCode).add(corpus);
            }
        }

        List<String> sortedAvailableLanguages = new ArrayList<String>(availableLangs.size());
        sortedAvailableLanguages.addAll(availableLangs);
        Collections.sort(sortedAvailableLanguages);
        for (String langCode : sortedAvailableLanguages) {
            String name = this.languages.nameForCode(langCode);
            if (name != null) {
                Comboitem item = this.languageSelect.appendItem(name);
                item.setValue(langCode);
            }
        }
//        for (String code : this.languages.getCodes()) {
//            Comboitem item = this.languageSelect.appendItem(this.languages.nameForCode(code));
//            item.setValue(code);
//        }
    }
}
