package eu.clarin.sru.fcs.aggregator.sopt;

import org.zkoss.zul.Treeitem;

/**
 *
 * @author Yana Panchenko
 */
public interface CorpusRendererI {
    
    public void selectChildren(Treeitem openedTreeitem);
    public void updateItem(Treeitem item, boolean select);

}
