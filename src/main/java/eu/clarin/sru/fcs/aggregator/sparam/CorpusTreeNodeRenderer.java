package eu.clarin.sru.fcs.aggregator.sparam;

import eu.clarin.sru.fcs.aggregator.data.Corpus;
import eu.clarin.sru.fcs.aggregator.data.Endpoint;
import eu.clarin.sru.fcs.aggregator.data.Institution;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treechildren;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.TreeitemRenderer;
import org.zkoss.zul.Treerow;

/**
 * Renders CorpusTreeNode data to the specified tree item.
 *
 * @author Yana Panchenko
 */
public class CorpusTreeNodeRenderer implements TreeitemRenderer {

    public static final String ITEM_DATA = "itemdata";
    public static final String ITEM_CHECKBOX = "itemcheckbox";


    @Override
    public void render(Treeitem item, Object data, int index) throws Exception {
        
        addOnOpenListener(item);
        CorpusTreeNode node = (CorpusTreeNode) data;
        if (data instanceof Institution) {
            item.setOpen(true);
//        } else if (data instanceof EndpointY) {
//            item.setOpen(true);
        }

        item.setAttribute(ITEM_DATA, data);
        Treerow row = new Treerow();
        row.setParent(item);
        Treecell cell = new Treecell();
        cell.setParent(row);
        if (data instanceof Institution) {
            cell.setLabel(node.toString());
        } else {
            Checkbox checkbox = new Checkbox(node.toString());
            item.setAttribute(ITEM_CHECKBOX, checkbox);
            addOnCheckListener(checkbox);
            checkbox.setParent(cell);
        }
    }

    public static void selectItem(Treeitem item) {
        Object checkboxAttr = item.getAttribute(ITEM_CHECKBOX);
        if (checkboxAttr != null) {
            Checkbox checkbox = (Checkbox) checkboxAttr;
            checkbox.setChecked(true);
            item.setSelected(true);
        }
    }

    public static void selectChildItems(Treeitem item) {
        Treechildren childItems = item.getTreechildren();
        if (childItems != null) {
            for (Treeitem childItem : childItems.getItems()) {
                Object checkboxAttr = childItem.getAttribute(ITEM_CHECKBOX);
                if (checkboxAttr != null) {
                    Checkbox checkbox = (Checkbox) checkboxAttr;
                    checkbox.setChecked(true);
                    childItem.setSelected(true);
                    selectChildItems(childItem);
                }
            }
        }
    }
    
    public static void unselectItem(Treeitem item) {
        Object checkboxAttr = item.getAttribute(ITEM_CHECKBOX);
        if (checkboxAttr != null) {
            Checkbox checkbox = (Checkbox) checkboxAttr;
            checkbox.setChecked(false);
            item.setSelected(false);
            item.setOpen(false);
        }
    }

    public static void unselectChildItems(Treeitem eventItem) {
        Treechildren childItems = eventItem.getTreechildren();
        if (childItems != null) {
            for (Treeitem childItem : childItems.getItems()) {
                Checkbox checkbox = (Checkbox) childItem.getAttribute(ITEM_CHECKBOX);
                checkbox.setChecked(false);
                childItem.setSelected(false);
                unselectChildItems(childItem);
            }
        }
    }

    public static void unselectParentItems(Treeitem eventItem) {
        Treeitem parentItem = eventItem.getParentItem();
        Object checkboxAttr = parentItem.getAttribute(ITEM_CHECKBOX);
        if (checkboxAttr != null) {
            Checkbox checkbox = (Checkbox) checkboxAttr;
            checkbox.setChecked(false);
            parentItem.setSelected(false);
            unselectParentItems(parentItem);
        }
    }

    private void addOnOpenListener(Treeitem item) {
        item.addEventListener("onOpen", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                Treeitem eventItem = (Treeitem) event.getTarget();
                Object checkboxAttr = eventItem.getAttribute(CorpusTreeNodeRenderer.ITEM_CHECKBOX);
                if (checkboxAttr != null) {
                    Checkbox eventCheckbox = (Checkbox) checkboxAttr;
                    if (eventCheckbox.isChecked()) {
                        CorpusTreeNodeRenderer.selectChildItems(eventItem);
                    }
                }
            }
        });
    }

    private void addOnCheckListener(Checkbox checkbox) {
        checkbox.addEventListener("onCheck", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                Checkbox eventCheckbox = (Checkbox) event.getTarget();
                Treeitem eventItem = (Treeitem) eventCheckbox.getParent().getParent().getParent();
                if (eventCheckbox.isChecked()) {
                    eventItem.setSelected(true);
                    selectChildItems(eventItem);
                } else {
                    eventItem.setSelected(false);
                    unselectChildItems(eventItem);
                    unselectParentItems(eventItem);
                }
            }
        });
    }
    
    
    
    public static void selectEndpoints(Tree tree, Map<String, List<String>> selectedEndpoints) {

        if (selectedEndpoints == null) {
            // select all open items
            Treechildren openTreeItems = tree.getTreechildren();
            for (Treeitem openItem : openTreeItems.getItems()) {
                selectItem(openItem);
            }
        } else {

            Treechildren institutionItems = tree.getTreechildren();
            if (institutionItems != null) {
                for (Treeitem institutionItem : institutionItems.getItems()) {
                    Object itemData = institutionItem.getAttribute(CorpusTreeNodeRenderer.ITEM_DATA);
                    if (itemData instanceof Institution) {
                        for (Treeitem endpointItem : getChildren(institutionItem)) {
                            Endpoint endpoint = (Endpoint) endpointItem.getAttribute(CorpusTreeNodeRenderer.ITEM_DATA);
                            if (selectedEndpoints.containsKey(endpoint.getUrl())) {
                                selectEndpoint(endpointItem, selectedEndpoints.get(endpoint.getUrl()));
                            }
                        }
                    }
                }
            }
        }
    }

    public static void selectEndpoint(Treeitem endpointItem, List<String> selectedCorpora) {

        if (selectedCorpora.isEmpty()) {
            Object checkboxAttr = endpointItem.getAttribute(CorpusTreeNodeRenderer.ITEM_CHECKBOX);
            if (checkboxAttr != null) {
                Checkbox checkbox = (Checkbox) checkboxAttr;
                checkbox.setChecked(true);
                endpointItem.setSelected(true);
                CorpusTreeNodeRenderer.selectChildItems(endpointItem);
            }
        } else {
            Set<String> requestedCorpora = new HashSet<String>();
            requestedCorpora.addAll(selectedCorpora);
            selectCorpora(endpointItem, //endpoint, 
                    requestedCorpora);

        }
    }

    public static void selectCorpora(Treeitem treeItem, Set<String> requestedCorpora) {
        if (requestedCorpora.isEmpty()) {
            return;
        }
        treeItem.setOpen(true);
        List<Treeitem> corpusItems = getChildren(treeItem);
        if (corpusItems != null) {
            for (Treeitem corpusItem : corpusItems) {
                Corpus corpus = (Corpus) corpusItem.getAttribute(CorpusTreeNodeRenderer.ITEM_DATA);
                if (requestedCorpora.contains(corpus.getValue())) {
                    Object checkboxAttr = corpusItem.getAttribute(CorpusTreeNodeRenderer.ITEM_CHECKBOX);
                    if (checkboxAttr != null) {
                        Checkbox checkbox = (Checkbox) checkboxAttr;
                        checkbox.setChecked(true);
                        corpusItem.setSelected(true);
                        CorpusTreeNodeRenderer.selectChildItems(corpusItem);
                    }
                    requestedCorpora.remove(corpus.getValue());
                    if (requestedCorpora.isEmpty()) {
                        break;
                    }
                }
            }
            for (Treeitem corpusItem : corpusItems) {
                if (!requestedCorpora.isEmpty()) {
                    selectCorpora(corpusItem, requestedCorpora);
                }
            }
        }
    }

        
    public static List<Treeitem> getChildren(Treeitem parentItem) {
        List<Treeitem> itemsCopy = new ArrayList<Treeitem>();
        Treechildren childItems = parentItem.getTreechildren();
        if (childItems != null) {
            for (Treeitem childItem : childItems.getItems()) {
                if (childItem.getParentItem() == parentItem) {
                    itemsCopy.add(childItem);
                }
            }
        }
        return itemsCopy;
    }
}
