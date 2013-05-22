/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.clarin.sru.fcs.aggregator.sparam2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.A;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.DefaultTreeNode;
import org.zkoss.zul.Label;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.TreeitemRenderer;
import org.zkoss.zul.Treerow;

/**
 *
 * @author Yana Panchenko <yana_panchenko at yahoo.com>
 */
/**
 * The structure of tree
 *
 * <pre>
 * &lt;treeitem>
 *   &lt;treerow>
 *     &lt;treecell>...&lt;/treecell>
 *   &lt;/treerow>
 *   &lt;treechildren>
 *     &lt;treeitem>...&lt;/treeitem>
 *   &lt;/treechildren>
 * &lt;/treeitem>
 * </pre>
 */
public class Corpus2Renderer implements TreeitemRenderer<DefaultTreeNode<Corpus2>> {

    Map<String, Set<Corpus2>> selectedCorpora = new HashMap<String,Set<Corpus2>>();

    
    @Override
    public void render(Treeitem treeItem, DefaultTreeNode<Corpus2> treeNode, int index) throws Exception {

        Treerow dataRow = new Treerow();
        dataRow.setParent(treeItem);
        treeItem.setValue(treeNode);
        treeItem.setOpen(false);
        Corpus2 data = treeNode.getData();

        if (data.isTemporary()) {
            System.out.println("IN TEMP NODE!!!!");
            return;
        }

        Treecell cell1 = createCellForSelectCorpus(data, dataRow);
        dataRow.appendChild(cell1);
        Treecell cell4 = createCellForHome(data);
        dataRow.appendChild(cell4);
        Treecell cell2 = createCellForCorpusLanguage(data);
        dataRow.appendChild(cell2);
        Treecell cell5 = createCellForCorpusInstitution(data);
        dataRow.appendChild(cell5);
        Treecell cell3 = createCellForCorpusDescription(data);
        dataRow.appendChild(cell3);

        treeItem.addEventListener(Events.ON_OPEN, new EventListener<Event>() {
            @Override
            public void onEvent(Event event) throws Exception {

                Treeitem openedTreeitem = (Treeitem) event.getTarget();
                if (openedTreeitem.getTreechildren().getChildren().isEmpty()) {
                    openedTreeitem.setOpen(false);
                } else {
                    // get first child
                    Treeitem childTreeitem = (Treeitem) openedTreeitem.getTreechildren().getChildren().get(0);
                    // if this first child is temp node remove it
                    DefaultTreeNode<Corpus2> openedNodeValue = (DefaultTreeNode<Corpus2>) openedTreeitem.getValue();
                    Corpus2 openedCorpus = openedNodeValue.getData();
                    DefaultTreeNode<Corpus2> childNodeValue = (DefaultTreeNode<Corpus2>) childTreeitem.getValue();
                    Corpus2 childCorpus = childNodeValue.getData();
                    if (childCorpus.isTemporary()) {
                        openedNodeValue.getChildren().remove(childTreeitem.getIndex());
                        // add real corpora nodes
                        Iterable<Corpus2> subcorpora = Corpus2.getSubcorpora(openedCorpus);
                        boolean open = false;
                        for (Corpus2 corpus : subcorpora) {
                            List<DefaultTreeNode<Corpus2>> tempChildren = new ArrayList<DefaultTreeNode<Corpus2>>(1);
                            Corpus2 tempChildCorpus = new Corpus2();
                            tempChildren.add(new DefaultTreeNode<Corpus2>(tempChildCorpus));
                            DefaultTreeNode<Corpus2> subcorporaNode = new DefaultTreeNode(corpus, tempChildren);
                            openedNodeValue.getChildren().add(subcorporaNode);
                            open = true;
                        }
                        openedTreeitem.setOpen(open);
                    }
                }
            }
        });


    }

    private Checkbox createCheckboxForSelectCorpus(Treerow dataRow) {
        Checkbox checkbox = new Checkbox();
        checkbox.setValue(dataRow);
        Treeitem eventItem = (Treeitem) dataRow.getParent();
        DefaultTreeNode<Corpus2> eventNode = (DefaultTreeNode<Corpus2>) eventItem.getValue();
        if (selectedCorpora.containsKey(eventNode.getData().getEndpointUrl())) {
            if (selectedCorpora.get(eventNode.getData().getEndpointUrl()).contains(eventNode.getData())) {
                checkbox.setChecked(true);
                toggleCorpusCheckbox(eventNode.getData(), checkbox, dataRow);
            }
        }
        checkbox.addEventListener("onCheck", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                Checkbox eventCheckbox = (Checkbox) event.getTarget();
                Treerow eventRow = (Treerow) eventCheckbox.getValue();
                Treeitem eventItem = (Treeitem) eventRow.getParent();
                DefaultTreeNode<Corpus2> eventNode = (DefaultTreeNode<Corpus2>) eventItem.getValue();
                toggleCorpusCheckbox(eventNode.getData(), eventCheckbox, eventRow);
            }
        });
        return checkbox;
    }

    private void toggleCorpusCheckbox(Corpus2 corpus, Checkbox checkbox, Treerow row) {
        if (checkbox.isChecked()) {
            row.setZclass("z-treerow z-treerow-seld");
            addToSelected(corpus);
        } else {
            row.setZclass("z-treerow");
            removeFromSelected(corpus);
        }
    }
    
    public void updateItem(Treeitem item, boolean select) {
        Treerow row = (Treerow) item.getFirstChild();
        Checkbox checkbox = (Checkbox) row.getFirstChild().getFirstChild();
        DefaultTreeNode<Corpus2> node = (DefaultTreeNode<Corpus2>) item.getValue();
        checkbox.setChecked(select);
        toggleCorpusCheckbox(node.getData(), checkbox, row);
    }
    

//    private Label createLabelForCorpus(Corpus2 data) {
//        Label label;
//        if (data.isRoot()) {
//            if (data.getInstitution() != null) {
//                label = new Label(data.getInstitution().getName());
//            } else {
//                label = new Label("");
//            }
//        } else {
//            label = new Label(data.getDisplayName());
//        }
//        return label;
//    }
//    private Treecell createCellForSelectCorpus(Corpus2 data, Treerow dataRow) {
//        Treecell cell = new Treecell();
//        Checkbox corpusCheckbox = createCheckboxForSelectCorpus(dataRow);
//        corpusCheckbox.setParent(cell);
//        if (data.getLandingPage() == null) {
//            Label corpusLabel = createLabelForCorpus(data);
//            corpusLabel.setParent(cell);
//        } else {
////            A link = new A(data.getDisplayName());
////            link.setHref(data.getLandingPage());
////            link.setParent(cell);
//            Label corpusLabel = new Label(data.getDisplayName());
//            corpusLabel.setParent(cell);
//            
////            A link = new A("");
////            link.setHref(data.getLandingPage());
////            link.setImage("go-home.png");
////            link.setParent(cell);
//        }
//        return cell;
//    }
    private Treecell createCellForSelectCorpus(Corpus2 data, Treerow dataRow) {
        Treecell cell = new Treecell();
        Checkbox corpusCheckbox = createCheckboxForSelectCorpus(dataRow);
        corpusCheckbox.setParent(cell);
        if (data.getDisplayName() == null) {
            Label corpusLabel = new Label("");
            corpusLabel.setParent(cell);
        } else {
            Label corpusLabel = new Label(data.getDisplayName());
            corpusLabel.setParent(cell);
        }
        return cell;
    }

    private Treecell createCellForCorpusInstitution(Corpus2 data) {
        Treecell cell = new Treecell();
        if (data.getInstitution() == null) {
            Label label = new Label("");
            label.setParent(cell);
        } else {
            Label label = new Label(data.getInstitution().getName());
            label.setParent(cell);
        }
        return cell;
    }

    private Treecell createCellForHome(Corpus2 data) {
        Treecell cell = new Treecell();
        if (data.getLandingPage() == null) {
            Label label = new Label("");
            label.setParent(cell);
        } else {

            A link = new A();
            link.setTarget("_blank");
            link.setHref(data.getLandingPage());
            link.setImage("go-home.png");
            link.setParent(cell);
        }
        return cell;
    }

    private Treecell createCellForCorpusLanguage(Corpus2 data) {
        Treecell cell = new Treecell();
        if (!data.getLanguages().isEmpty()) {
            StringBuilder langs = new StringBuilder();
            for (String lang : data.getLanguages()) {
                langs.append(lang);
                langs.append(" ");
            }
            Label label = new Label(langs.toString());
            label.setParent(cell);
        } else {
            Label label = new Label("");
            label.setParent(cell);
        }
        return cell;
    }

    private Treecell createCellForCorpusDescription(Corpus2 data) {
        Treecell cell = new Treecell();
        if (data.getDescription() != null) {
            //if (data.getDescription().length() > 40) {
            //    String descrStart = data.getDescription().substring(0, 35) + " ...";
            //    Label label = new Label(descrStart);
            //    label.setTooltiptext(data.getDescription());
            //    label.setParent(cell);
            //} else {
            Label label = new Label(data.getDescription());
            label.setParent(cell);
            //}
        } else {
            Label label = new Label("");
            label.setParent(cell);
        }

        return cell;
    }

    private void addToSelected(Corpus2 data) {
        if (!this.selectedCorpora.containsKey(data.getEndpointUrl())) {
            this.selectedCorpora.put(data.getEndpointUrl(), new HashSet<Corpus2>());
        }
        this.selectedCorpora.get(data.getEndpointUrl()).add(data);

    }

    private void removeFromSelected(Corpus2 data) {
        if (this.selectedCorpora.containsKey(data.getEndpointUrl())) {
            this.selectedCorpora.get(data.getEndpointUrl()).remove(data);
        }
    }

    public Map<String, Set<Corpus2>> getSelectedCorpora() {
        return selectedCorpora;
    }
}
