package eu.clarin.sru.fcs.aggregator.sparam;

import java.util.List;

/**
 * CorpusTreeNode interface to be used with CorpusTreeNode
 *
 * @author Yana Panchenko
 */
public interface CorpusTreeNode {

    public boolean hasChildrenLoaded();

    public void loadChildren();
        
    public List<? extends CorpusTreeNode> getChildren();

    public CorpusTreeNode getChild(int index);

}
