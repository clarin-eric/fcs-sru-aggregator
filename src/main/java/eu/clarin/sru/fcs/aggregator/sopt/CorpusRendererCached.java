package eu.clarin.sru.fcs.aggregator.sopt;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.DefaultTreeNode;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.TreeitemRenderer;
import org.zkoss.zul.Treerow;

/**
 * Renders treeitem of corpora tree from corpora cache. I.e. corpora sub-resources
 * are pre-loaded and the information about them is taken from CorpusModelCached,
 * which accesses the cache. 
 *
 * @author Yana Panchenko
 *
 */
public class CorpusRendererCached extends CorpusRenderer
        implements TreeitemRenderer<DefaultTreeNode<Corpus>> {

    public CorpusRendererCached(CorpusModelCached model) {
        super(model);
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
                if (model.isCorpusSelected((DefaultTreeNode<Corpus>) openedTreeitem.getValue())) {
                    selectChildren(openedTreeitem);
                }
            }
        });

    }

}
