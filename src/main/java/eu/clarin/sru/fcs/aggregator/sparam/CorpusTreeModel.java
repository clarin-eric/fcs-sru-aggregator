/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.clarin.sru.fcs.aggregator.sparam;

import org.zkoss.zul.AbstractTreeModel;
import org.zkoss.zul.ext.TreeSelectableModel;

/**
 * TreeModel for CorpusTreeNode objects. Loads Institution and Endpoint nodes
 * immediately. Loads Corpus child nodes only when the parent node (Endpoint or
 * Corpus) is selected. I.e. if a user doesn't want to select a specific corpus,
 * he doesn't have to wait till the whole tree is loaded.
 * 
 * @author Yana Panchenko
 */
public class CorpusTreeModel extends AbstractTreeModel<CorpusTreeNode> implements TreeSelectableModel {
 
 
    public CorpusTreeModel(CorpusTreeNode root) {
        super(root);
    }
    
    @Override
    public CorpusTreeNode getChild(CorpusTreeNode parent, int index) {
        if (!parent.hasChildrenLoaded()) {
            parent.loadChildren();
        }
        if (index >= parent.getChildren().size()) {
            return null;
        } else {
            return parent.getChildren().get(index);
        }
    }
 
    @Override
    public int getChildCount(CorpusTreeNode parent) {
        int count = 0;
        while (parent.getChild(count) != null) {
            count++;
        }
        return count;
    }
 
    @Override
    public boolean isLeaf(CorpusTreeNode node) {
        return (getChildCount(node) == 0);
    }
 
}