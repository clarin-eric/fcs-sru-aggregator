package eu.clarin.sru.fcs.aggregator.sopt;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.DefaultTreeNode;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.TreeitemRenderer;
import org.zkoss.zul.Treerow;

/**
 * Renders treeitem of corpora tree in a 'live' mode. I.e. corpora sub-resources
 * are loaded on demand. It is necessary that CorpusModelLive is used with this
 * renderer.
 *
 * TODO: remove the loaded sub-resources from the tree when they are
 * closed by the user?
 *
 * @author Yana Panchenko
 */
public class CorpusRendererLive extends CorpusRenderer
        implements TreeitemRenderer<DefaultTreeNode<Corpus>> {

    public CorpusRendererLive(CorpusModelLive model) {
        super(model);
    }

    @Override
    public void render(Treeitem treeItem, DefaultTreeNode<Corpus> treeNode, int index) throws Exception {

        Treerow dataRow = new Treerow();
        dataRow.setParent(treeItem);
        treeItem.setValue(treeNode);
        treeItem.setOpen(false);
        Corpus data = treeNode.getData();

        if (data.isTemporary()) {
            return;
        }
        addCorpusDataIntoRow(dataRow, data);

        treeItem.addEventListener(Events.ON_OPEN, new EventListener<Event>() {
            @Override
            public void onEvent(Event event) throws Exception {

                Treeitem openedTreeitem = (Treeitem) event.getTarget();
                if (openedTreeitem.isOpen()) {
                    if (model.hasChildren(openedTreeitem)) {
                        ((CorpusModelLive) model).loadChildren(openedTreeitem);
                        openedTreeitem.setOpen(model.hasChildren(openedTreeitem));
                    } else {
                        openedTreeitem.setOpen(false);
                    }
                }

                if (model.isCorpusSelected((DefaultTreeNode<Corpus>) openedTreeitem.getValue())) {
                    selectChildren(openedTreeitem);
                }
            }
        });

    }
}
