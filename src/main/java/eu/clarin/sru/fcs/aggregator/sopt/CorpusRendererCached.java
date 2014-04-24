package eu.clarin.sru.fcs.aggregator.sopt;

import eu.clarin.sru.fcs.aggregator.app.WebAppListener;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.A;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.DefaultTreeNode;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treechildren;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.TreeitemRenderer;
import org.zkoss.zul.Treerow;

/**
 *
 * @author Yana Panchenko
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
public class CorpusRendererCached implements TreeitemRenderer<DefaultTreeNode<Corpus>>, CorpusRendererI  {

    private Languages languages;
    private CorpusModelCached model;

    public CorpusRendererCached(CorpusModelCached model) {
        languages = (Languages) Executions.getCurrent().getDesktop().getWebApp().getAttribute(WebAppListener.LANGUAGES);
        this.model = model;
    }

    @Override
    public void render(Treeitem treeItem, DefaultTreeNode<Corpus> treeNode, int index) throws Exception {

        Treerow dataRow = new Treerow();
        dataRow.setParent(treeItem);
        treeItem.setValue(treeNode);
        treeItem.setOpen(false);
        Corpus data = treeNode.getData();
        addCorpusDataIntoRow(dataRow, data);

        treeItem.addEventListener(Events.ON_OPEN, new EventListener<Event>() {
            @Override
            public void onEvent(Event event) throws Exception {

                Treeitem openedTreeitem = (Treeitem) event.getTarget();
                //if (openedTreeitem.isOpen()) {
                //    if (model.hasChildren(openedTreeitem)) {
                //        model.loadChildren(openedTreeitem);
                //        openedTreeitem.setOpen(model.hasChildren(openedTreeitem));
                //    } else {
                //        openedTreeitem.setOpen(false);
                //    }
                //}

                if (model.isCorpusSelected((DefaultTreeNode<Corpus>) openedTreeitem.getValue())) {
                    selectChildren(openedTreeitem);
                }
            }
        });

    }

    //TODO check if it's working only on open children???
    public void selectChildren(Treeitem openedTreeitem) {

        for (Component comp : openedTreeitem.getChildren()) {
            if (comp instanceof Treechildren) {
                Treechildren item = (Treechildren) comp;
                for (Treeitem childItem : item.getItems()) {
                    updateItem(childItem, true);
                }
                break;
            }
        }

    }

    public void updateItem(Treeitem item, boolean select) {

        for (Component comp : item.getChildren()) {
            // update the item row
            if (comp instanceof Treerow) {
                Treerow row = (Treerow) comp;
                Checkbox checkbox = (Checkbox) row.getFirstChild().getFirstChild();
                DefaultTreeNode<Corpus> node = (DefaultTreeNode<Corpus>) item.getValue();
                checkbox.setChecked(select);
                toggleCorpusCheckbox(node.getData(), checkbox, row);
                // update the item children
            } else if (comp instanceof Treechildren) {
                Treechildren children = (Treechildren) comp;
                for (Treeitem childItem : children.getItems()) {
                    updateItem(childItem, select);
                }
            }
        }

    }

    private void updateParentItem(Treeitem item, boolean checked) {

        // if item becomes unselected, unselect it parent
        if (!checked) {
            Treeitem parent = item.getParentItem();
            if (parent != null) {
                for (Component comp : parent.getChildren()) {
                    // update the item row
                    if (comp instanceof Treerow) {
                        Treerow row = (Treerow) comp;
                        Checkbox checkbox = (Checkbox) row.getFirstChild().getFirstChild();
                        DefaultTreeNode<Corpus> node = (DefaultTreeNode<Corpus>) parent.getValue();
                        checkbox.setChecked(checked);
                        toggleCorpusCheckbox(node.getData(), checkbox, row);
                        updateParentItem(parent, checked);
                        break;
                    }
                }

            }
        }
    }

    private void toggleCorpusCheckbox(Corpus corpus, Checkbox checkbox, Treerow row) {
        if (checkbox.isChecked()) {
            row.setZclass("z-treerow z-treerow-seld");
            model.addToSelected(corpus);
        } else {
            row.setZclass("z-treerow");
            model.removeFromSelected(corpus);
        }
    }

    private void addCorpusDataIntoRow(Treerow dataRow, Corpus data) {
        Treecell cell1 = createCellForSelectCorpus(data, dataRow);
        dataRow.appendChild(cell1);
        Treecell cell4 = createCellForHome(data);
        dataRow.appendChild(cell4);
        Treecell cell2 = createCellForCorpusLanguage(data);
        dataRow.appendChild(cell2);
        Treecell cell6 = createCellForCorpusNumberOfRecords(data);
        dataRow.appendChild(cell6);
        Treecell cell5 = createCellForCorpusInstitution(data);
        dataRow.appendChild(cell5);
        Treecell cell3 = createCellForCorpusDescription(data);
        dataRow.appendChild(cell3);
    }

    private Treecell createCellForSelectCorpus(Corpus data, Treerow dataRow) {
        Treecell cell = new Treecell();
        Checkbox corpusCheckbox = createCheckboxForSelectCorpus(dataRow);
        corpusCheckbox.setParent(cell);
        if (data.getDisplayName() == null) {
            Label corpusLabel = new Label("");
            corpusLabel.setParent(cell);
        } else {
            String name = data.getDisplayName();
            // TODO: this is temp, before some endpoint
            // fix long names for their resources, that look 
            // more like a description
            if (name.length() > 50) {
                int firstWordEnd = name.indexOf(" ");
                if (firstWordEnd > 0) {
                    data.setDisplayName(name.substring(0, firstWordEnd));
                    if (data.getDescription() == null) {
                        data.setDescription(name);
                    }
                }
            }
            Label corpusLabel = new Label(data.getDisplayName());
            corpusLabel.setParent(cell);
        }
        return cell;
    }

    private Checkbox createCheckboxForSelectCorpus(Treerow dataRow) {
        Checkbox checkbox = new Checkbox();
        checkbox.setValue(dataRow);
        Treeitem eventItem = (Treeitem) dataRow.getParent();
        DefaultTreeNode<Corpus> eventNode = (DefaultTreeNode<Corpus>) eventItem.getValue();
        if (model.isSelected(eventNode)) {
            checkbox.setChecked(true);
            toggleCorpusCheckbox(eventNode.getData(), checkbox, dataRow);
        }
        checkbox.addEventListener("onCheck", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                Checkbox eventCheckbox = (Checkbox) event.getTarget();
                Treerow eventRow = (Treerow) eventCheckbox.getValue();
                Treeitem eventItem = (Treeitem) eventRow.getParent();
                DefaultTreeNode<Corpus> eventNode = (DefaultTreeNode<Corpus>) eventItem.getValue();
                toggleCorpusCheckbox(eventNode.getData(), eventCheckbox, eventRow);
                updateParentItem(eventItem, eventCheckbox.isChecked());
                for (Treeitem item : eventItem.getTreechildren().getItems()) {
                    updateItem(item, eventCheckbox.isChecked());
                }
            }
        });
        return checkbox;
    }

    private Treecell createCellForCorpusInstitution(Corpus data) {
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
    
    private Treecell createCellForCorpusNumberOfRecords(Corpus data) {
        Treecell cell = new Treecell();
        if (data.getNumberOfRecords() == null) {
            Label label = new Label("");
            label.setParent(cell);
        } else {
            Label label = new Label(data.getNumberOfRecords().toString());
            label.setParent(cell);
        }
        return cell;
    }

    private Treecell createCellForHome(Corpus data) {
        Treecell cell = new Treecell();
        if (data.getLandingPage() == null) {
            Label label = new Label("");
            label.setParent(cell);
        } else {

            A link = new A();
            link.setTarget("_blank");
            link.setHref(data.getLandingPage());
            link.setImage("img/go-home.png");
            link.setParent(cell);
        }
        return cell;
    }

//    private Treecell createCellForCorpusLanguage(Corpus data) {
//        Treecell cell = new Treecell();
//        if (!data.getLanguages().isEmpty()) {
//            StringBuilder langs = new StringBuilder();
//            for (String lang : data.getLanguages()) {
//                String langName = languages.nameForCode(lang);
//                if (langName != null) {
//                    langs.append(langName);
//                } else {
//                    langs.append(lang);
//                }
//                langs.append("\n ");
//
//            }
//            Label label = new Label(langs.toString());
//            label.setMultiline(true);
//            label.setParent(cell);
//        } else {
//            Label label = new Label("");
//            label.setParent(cell);
//        }
//        return cell;
//    }
    
     private Treecell createCellForCorpusLanguage(Corpus data) {
        Treecell cell = new Treecell();
        if (data.getLanguages().isEmpty()) {
            Label label = new Label("");
            label.setParent(cell);
        } else if (data.getLanguages().size() == 1) {
            Label label = new Label(languages.nameForCode(data.getLanguages().iterator().next()));
            label.setParent(cell);
        } else {
            Groupbox langPanel = new Groupbox();
            langPanel.setMold("3d");
            langPanel.setContentStyle("border:0");
            langPanel.setClosable(true);
            langPanel.setOpen(false);
            langPanel.setTitle("multiple");
            StringBuilder langs = new StringBuilder();
            for (String lang : data.getLanguages()) {
                String langName = languages.nameForCode(lang);
                if (langName != null) {
                    langs.append(langName);
                } else {
                    langs.append(lang);
                }
                langs.append("\n ");

            }
            Label label = new Label(langs.toString());
            label.setMultiline(true);
            label.setParent(langPanel);
            langPanel.setParent(cell);
            
        }
        return cell;
    }

    private Treecell createCellForCorpusDescription(Corpus data) {
        Treecell cell = new Treecell();
        if (data.getDescription() != null) {
            int okLength = 125;
            if (data.getDescription().length() > okLength) {
                String descrStart = // data.getDescription().substring(0, 35) + " ...";
                        getDisplayedText(data.getDescription(), okLength);
                Label label = new Label(descrStart);
                label.setTooltiptext(data.getDescription());
                label.setParent(cell);
            } else {
            Label label = new Label(data.getDescription());
            label.setMultiline(true);
            label.setParent(cell);
            
            //ideally should contain html5 <details> element, but only
            //chrome supports it as of now:
            //<details>
            //<summary>First sentence</summary>
            //Rest sentences.
	    //</details>
            
            
            }
        } else {
            Label label = new Label("");
            label.setParent(cell);
        }

        return cell;
    }

    private String getDisplayedText(String description, int okLength) {
        int dotIndex = description.indexOf(" ", okLength);
        if (dotIndex < 0 || dotIndex > okLength + 50) {
            dotIndex = okLength;
        }
        return description.substring(0, dotIndex) + "...";
    }
}
