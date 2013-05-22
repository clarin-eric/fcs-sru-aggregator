/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.clarin.sru.fcs.aggregator.sparam2;

import java.util.Arrays;
import org.zkoss.zul.DefaultTreeModel;
import org.zkoss.zul.DefaultTreeNode;

/**
 *
 * @author Yana Panchenko <yana_panchenko at yahoo.com>
 */
public class CorpusTreeModel2 extends DefaultTreeModel<Corpus2> { 
    
    DefaultTreeNode<Corpus2> root;
    
    public CorpusTreeModel2(DefaultTreeNode<Corpus2> treeNode) {
        super(treeNode);
        root = treeNode;
    }
    
    /**
     * remove the nodes which parent is <code>parent</code> with indexes
     * <code>indexes</code>
     * 
     * @param parent
     *            The parent of nodes are removed
     * @param indexFrom
     *            the lower index of the change range
     * @param indexTo
     *            the upper index of the change range
     * @throws IndexOutOfBoundsException
     *             - indexFrom < 0 or indexTo > number of parent's children
     */
    public void remove(DefaultTreeNode<Corpus2> parent, int indexFrom, int indexTo) {
        for (int i = indexTo; i >= indexFrom; i--) {
           parent.getChildren().remove(i);
        }
    }
    
    /**
     * remove the nodes which parent is <code>parent</code> with indexes
     * <code>indexes</code>
     * 
     * @param parent
     *            The parent of nodes are removed
     * @param indexFrom
     *            the lower index of the change range
     * @param indexTo
     *            the upper index of the change range
     * @throws IndexOutOfBoundsException
     *             - indexFrom < 0 or indexTo > number of parent's children
     */
    public void remove(DefaultTreeNode<Corpus2> parent, int index) {
        parent.getChildren().remove(index);
    }
    
    /**
     * insert new nodes which parent is <code>parent</code> with indexes
     * <code>indexes</code> by new nodes <code>newNodes</code>
     * 
     * @param parent
     *            The parent of nodes are inserted
     * @param indexFrom
     *            the lower index of the change range
     * @param indexTo
     *            the upper index of the change range
     * @param newNodes
     *            New nodes which are inserted
     * @throws IndexOutOfBoundsException
     *             - indexFrom < 0 or indexTo > number of parent's children
     */
    public void insert(DefaultTreeNode<Corpus2> parent, int indexFrom, int indexTo, DefaultTreeNode<Corpus2>[] newNodes) {
        for (int i = indexFrom; i <= indexTo; i++) {
           parent.getChildren().add(i, newNodes[i - indexFrom]);
        }
    }
    
    /**
     * append new nodes which parent is <code>parent</code> by new nodes
     * <code>newNodes</code>
     * 
     * @param parent
     *            The parent of nodes are appended
     * @param newNodes
     *            New nodes which are appended
     */
    public void add(DefaultTreeNode<Corpus2> parent, DefaultTreeNode<Corpus2>[] newNodes) {
        parent.getChildren().addAll(Arrays.asList(newNodes));
    }
}
