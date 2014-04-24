package eu.clarin.sru.fcs.aggregator.sopt;

import java.util.Map;
import java.util.Set;
import org.zkoss.zul.DefaultTreeNode;
import org.zkoss.zul.Treeitem;

/**
 * Represents corpora model that has access to Corpus objects and their 
 * information about corpora.
 * 
 * @author Yana Panchenko
 */
public interface CorpusModelI {

    
    public boolean isCorpusSelected(DefaultTreeNode<Corpus> node);

    /**
     * Remove the nodes which parent is
     * <code>parent</code> with indexes
     * <code>indexes</code>
     *
     * @param parent The parent of nodes are removed
     * @param indexFrom the lower index of the change range
     * @param indexTo the upper index of the change range
     * @throws IndexOutOfBoundsException - indexFrom < 0 or indexTo > number of
     * parent's children
     */
    public void remove(DefaultTreeNode<Corpus> parent, int indexFrom, int indexTo);

    /**
     * Remove the nodes which parent is
     * <code>parent</code> with indexes
     * <code>indexes</code>
     *
     * @param parent The parent of nodes are removed
     * @param indexFrom the lower index of the change range
     * @param indexTo the upper index of the change range
     * @throws IndexOutOfBoundsException - indexFrom < 0 or indexTo > number of
     * parent's children
     */
    public void remove(DefaultTreeNode<Corpus> parent, int index);

    /**
     * Insert new nodes which parent is
     * <code>parent</code> with indexes
     * <code>indexes</code> by new nodes
     * <code>newNodes</code>
     *
     * @param parent The parent of nodes are inserted
     * @param indexFrom the lower index of the change range
     * @param indexTo the upper index of the change range
     * @param newNodes New nodes which are inserted
     * @throws IndexOutOfBoundsException - indexFrom < 0 or indexTo > number of
     * parent's children
     */
    public void insert(DefaultTreeNode<Corpus> parent, int indexFrom, int indexTo, DefaultTreeNode<Corpus>[] newNodes);

    /**
     * Append new nodes which parent is
     * <code>parent</code> by new nodes
     * <code>newNodes</code>
     *
     * @param parent The parent of nodes are appended
     * @param newNodes New nodes which are appended
     */
    public void add(DefaultTreeNode<Corpus> parent, DefaultTreeNode<Corpus>[] newNodes);

    /**
     * Append new node which parent is
     * <code>parent</code> by new node
     * <code>newNode</code>
     *
     * @param parent The parent of appended node
     * @param newNode New node which is appended
     */
    public void add(DefaultTreeNode<Corpus> parent, DefaultTreeNode<Corpus> newNode);

    public boolean hasChildren(Treeitem item);



    /**
     * Append new node which parent is
     * <code>parent</code> by a new node that contains
     * <code>corpus</code>
     *
     * @param parent The parent of appended node
     * @param corpus The corpus that the new appended node should contain
     */
    public void add(DefaultTreeNode<Corpus> parent, Corpus corpus);

    public void addToSelected(Corpus data);

    public void removeFromSelected(Corpus data);

    public Map<String, Set<Corpus>> getSelectedCorpora();
 
}
