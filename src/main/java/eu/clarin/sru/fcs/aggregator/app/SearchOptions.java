package eu.clarin.sru.fcs.aggregator.app;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import eu.clarin.sru.client.SRUVersion;
import eu.clarin.sru.fcs.aggregator.sopt.CenterRegistryLive;
import eu.clarin.sru.fcs.aggregator.sopt.CenterRegistryForTesting;
import eu.clarin.sru.fcs.aggregator.sopt.CenterRegistryI;
import eu.clarin.sru.fcs.aggregator.sopt.Corpus;
import eu.clarin.sru.fcs.aggregator.sopt.CorpusByInstitutionComparator;
import eu.clarin.sru.fcs.aggregator.sopt.CorpusByInstitutionDComparator;
import eu.clarin.sru.fcs.aggregator.sopt.CorpusByNameComparator;
import eu.clarin.sru.fcs.aggregator.sopt.CorpusByNameDComparator;
import eu.clarin.sru.fcs.aggregator.sopt.CorpusModelCached;
import eu.clarin.sru.fcs.aggregator.sopt.CorpusModelI;
import eu.clarin.sru.fcs.aggregator.sopt.CorpusModelLive;
import eu.clarin.sru.fcs.aggregator.sopt.CorpusRendererCached;
import eu.clarin.sru.fcs.aggregator.sopt.CorpusRendererI;
import eu.clarin.sru.fcs.aggregator.sopt.CorpusRendererLive;
import eu.clarin.sru.fcs.aggregator.sopt.Languages;
import eu.clarin.sru.fcs.aggregator.cache.ScanCache;
import eu.clarin.sru.fcs.aggregator.util.SRUCQL;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
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
import org.zkoss.zul.DefaultTreeNode;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treechildren;
import org.zkoss.zul.Treecol;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.event.ZulEvents;

/**
 * Class representing Search Options page. Displays and let's user select corpora,
 * languages, number of records per page to be displayed, etc.
 * 
 * @author Yana Panchenko
 */
public class SearchOptions extends SelectorComposer<Component> {

    private static final Logger LOGGER = Logger.getLogger(SearchOptions.class.getName());
    
    @Wire
    private Combobox languageSelect;
    @Wire
    private Comboitem anyLanguage;
    private Languages languages;
    
    @Wire
    private Combobox maximumRecordsSelect;
    
    @Wire
    private Groupbox allCorpora;
    @Wire
    private Tree tree;
    private Map<String, List<String>> xAggregationContext;
    
    private CorpusModelI corporaModel;
    private CorpusRendererI corpusRenderer;
    
    private boolean liveMode = false;

    private ScanCache cache;
    
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        setUpAggerationContext();
        cache = (ScanCache) Executions.getCurrent().getDesktop().getWebApp().getAttribute(WebAppListener.CORPUS_CACHE);
        //if (cache.isEmpty()) {
        //    liveMode = true;
        //}
        setUpCorpusTree();
        languages = (Languages) Executions.getCurrent().getDesktop().getWebApp().getAttribute(WebAppListener.LANGUAGES);
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
        String selectedLang = cbox.getSelectedItem().getValue();
        for (Component comp : tree.getTreechildren().getChildren()) {
            Treeitem treeitem = (Treeitem) comp;
            DefaultTreeNode<Corpus> node = (DefaultTreeNode<Corpus>) treeitem.getValue();
            Corpus corpus = node.getData();
            if (corpus.getLanguages().contains(selectedLang) || selectedLang.equals(Languages.ANY_LANGUAGE_NAME)) {
                treeitem.setVisible(true);
            } else {
                corpusRenderer.updateItem(treeitem, false);
                treeitem.setVisible(false);
            }
        }
    }

    @Listen(ZulEvents.ON_AFTER_RENDER + "=#tree")
    public void onAfterRenderCorporaTree(Event ev) {
        if (this.xAggregationContext == null) {
            onSelectAll(null);
        } else {
            selectCorpora(xAggregationContext);
        }
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
            
            DefaultTreeNode<Corpus> node = (DefaultTreeNode<Corpus>) openItem.getValue();
            Corpus data = node.getData();
            System.out.println(data);
            
            corpusRenderer.updateItem(openItem, false);
        }
    }

    void selectCorpora(Map<String, List<String>> xAggregationContext) {
        onDeselectAll(null);
        Treechildren openTreeItems = tree.getTreechildren();
        
        List<Treeitem> openitems = new ArrayList<Treeitem>();
        openitems.addAll(openTreeItems.getItems());
            
        for (Treeitem openItem : openitems) {
            DefaultTreeNode<Corpus> node = (DefaultTreeNode<Corpus>) openItem.getValue();
            Corpus data = node.getData();
            List<String> handles = xAggregationContext.get(data.getEndpointUrl());
            if (handles == null) {
                if (data.getEndpointUrl().endsWith("/")) {
                    handles = xAggregationContext.get(
                            data.getEndpointUrl().substring(0, data.getEndpointUrl().length() - 1));
                } else {
                    handles = xAggregationContext.get(
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
                    
//                    // this is a temporary solution,
//                    // the whole concept on how selected externally resources
//                    // are displayed has to be changed, otherwise endpoints with
//                    // hundreds of subresources will break the tree interface
//                    // by loading all their resources...
//                    List<String> handlesCopy = new ArrayList<String>(handles.size());
//                    handlesCopy.addAll(handles);
//                    selectCorpora(openItem, data, handlesCopy);
                }
            }
        }
    }

    public int getMaxRecords() {
        return Integer.parseInt(maximumRecordsSelect.getValue());
    }

    public Map<String, Set<Corpus>> getSelectedCorpora() {
        return corporaModel.getSelectedCorpora();
    }

    private void loadLanguages() {

        Set<String> availableLangs = new HashSet<String>();

        Treechildren treeItems = tree.getTreechildren();
        for (Treeitem item : treeItems.getItems()) {
            DefaultTreeNode<Corpus> node = item.getValue();
            Corpus corpus = node.getData();
            for (String langCode : corpus.getLanguages()) {
                availableLangs.add(langCode);
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
    }
    
    public String getSearchLang() {
        return this.languageSelect.getSelectedItem().getValue();
    }

    private boolean isTestingOn() {
        boolean testingOn = false;
        String[] paramValue = Executions.getCurrent().getParameterMap().get("mode");
        if (paramValue != null) {
            String mode = paramValue[0].trim();
            if (mode.equals("testing")) {
                testingOn = true;
            }
        }
        return testingOn;
    }

    private void setUpAggerationContext() {
        String[] paramValue = Executions.getCurrent().getParameterMap().get(SRUCQL.AGGREGATION_CONTEXT);
        String contextJson = null;
        if (paramValue != null) {
            contextJson = paramValue[0].trim();
        }
        LOGGER.log(Level.INFO, "Received parameter {0}:[{1}], ", new String[]{SRUCQL.AGGREGATION_CONTEXT, contextJson});

        if (contextJson != null) {
            Gson gson = new Gson();
            Type mapType = new TypeToken<LinkedHashMap<String, ArrayList<String>>>() {
            }.getType();
            try {
                this.xAggregationContext = gson.fromJson(contextJson, mapType);
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Error parsing JSON from x-aggregation-context: {0} {1}", new String[]{ex.getMessage(), contextJson});
                Messagebox.show("Error in " + SRUCQL.AGGREGATION_CONTEXT, "FCS", 0, Messagebox.INFORMATION);
            }
        }
    }

    private void setUpCorpusTree() {
        if (isTestingOn()) {
            CenterRegistryI registry = new CenterRegistryForTesting();
            CorpusModelLive model = new CorpusModelLive(registry);
            CorpusRendererLive renderer = new CorpusRendererLive(model);
            model.setMultiple(true);
            tree.setModel(model);
            tree.setItemRenderer(renderer);
            this.corporaModel = model;
            this.corpusRenderer = renderer;
        } else if (liveMode) {
            CenterRegistryI registry = new CenterRegistryLive();
            CorpusModelLive model = new CorpusModelLive(registry);
            CorpusRendererLive renderer = new CorpusRendererLive(model);
            model.setMultiple(true);
            tree.setModel(model);
            tree.setItemRenderer(renderer);
            this.corporaModel = model;
            this.corpusRenderer = renderer;
        } else { // cached mode
            CorpusModelCached model = new CorpusModelCached(cache);
            CorpusRendererCached renderer = new CorpusRendererCached(model);
            model.setMultiple(true);
            tree.setModel(model);
            tree.setItemRenderer(renderer);
            this.corporaModel = model;
            this.corpusRenderer = renderer;
        }
        Treecol nameCol = (Treecol) tree.getTreecols().getFellow("nameCol");
        nameCol.setSortAscending(new CorpusByNameComparator());
        nameCol.setSortDescending(new CorpusByNameDComparator());
        Treecol instCol = (Treecol) tree.getTreecols().getFellow("instCol");
        instCol.setSortAscending(new CorpusByInstitutionComparator());
        instCol.setSortDescending(new CorpusByInstitutionDComparator());
        //tree.setSizedByContent(true);
    }


    private void selectCorpora(Treeitem openItem, Corpus data, List<String> handles) {
        List<String> handlesFound = new ArrayList<String>();
        for (String handle : handles) {
            if (handle.equals(data.getHandle())) {
                handlesFound.add(handle);
            }
        }
        for (String handle : handlesFound) {
            corpusRenderer.updateItem(openItem, true);
            handles.remove(handle);
        }
        
        if (!handles.isEmpty()) {
            int sizeBefore = handles.size();
            openItem.setOpen(true);
            Treechildren tchildren = openItem.getTreechildren();
            List<Treeitem> tcitems = new ArrayList<Treeitem>();
            tcitems.addAll(tchildren.getItems());
            for (Treeitem child : tcitems) {
                DefaultTreeNode<Corpus> node = (DefaultTreeNode<Corpus>) child.getValue();
                Corpus cdata = node.getData();
                selectCorpora(child, cdata, handles);
                if (handles.isEmpty()) {
                    break;
                }
            }
            if (sizeBefore == handles.size()) {
                openItem.setOpen(false);
            }
        }
    }

}