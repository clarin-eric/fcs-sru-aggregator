package eu.clarin.sru.fcs.aggregator.sopt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.tree.DefaultTreeModel;
import org.zkoss.zul.DefaultTreeNode;
import org.zkoss.zul.Treeitem;

/**
 *
 * @author Yana Panchenko
 */
public class CorpusModelCached extends org.zkoss.zul.DefaultTreeModel<Corpus> implements CorpusModelI {

    private static final Logger logger = Logger.getLogger(CorpusModelCached.class.getName());
    private Map<String, Set<Corpus>> selectedCorpora = new HashMap<String, Set<Corpus>>();
    private CorporaScanCache cache;

    public CorpusModelCached(CorporaScanCache cache) {
        super(new DefaultTreeNode(new Corpus(), new ArrayList<DefaultTreeNode<Corpus>>()));
        this.cache = cache;
        initCorpusTree();
    }

    @Override
    public boolean isCorpusSelected(DefaultTreeNode<Corpus> node) {
        if (selectedCorpora.containsKey(node.getData().getEndpointUrl())) {
            if (selectedCorpora.get(node.getData().getEndpointUrl()).contains(node.getData())) {
                return true;
            }
        }
        return false;
    }

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
    @Override
    public void remove(DefaultTreeNode<Corpus> parent, int indexFrom, int indexTo) {
        for (int i = indexTo; i >= indexFrom; i--) {
            parent.getChildren().remove(i);
        }
    }

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
    @Override
    public void remove(DefaultTreeNode<Corpus> parent, int index) {
        parent.getChildren().remove(index);
    }

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
    @Override
    public void insert(DefaultTreeNode<Corpus> parent, int indexFrom, int indexTo, DefaultTreeNode<Corpus>[] newNodes) {
        for (int i = indexFrom; i <= indexTo; i++) {
            parent.getChildren().add(i, newNodes[i - indexFrom]);
        }
    }

    /**
     * Append new nodes which parent is
     * <code>parent</code> by new nodes
     * <code>newNodes</code>
     *
     * @param parent The parent of nodes are appended
     * @param newNodes New nodes which are appended
     */
    @Override
    public void add(DefaultTreeNode<Corpus> parent, DefaultTreeNode<Corpus>[] newNodes) {
        parent.getChildren().addAll(Arrays.asList(newNodes));
    }

    /**
     * Append new node which parent is
     * <code>parent</code> by new node
     * <code>newNode</code>
     *
     * @param parent The parent of appended node
     * @param newNode New node which is appended
     */
    @Override
    public void add(DefaultTreeNode<Corpus> parent, DefaultTreeNode<Corpus> newNode) {
        parent.getChildren().add(newNode);
    }

    @Override
    public boolean hasChildren(Treeitem item) {
        return !item.getTreechildren().getChildren().isEmpty();
    }

    /**
     * Append new node which parent is
     * <code>parent</code> by a new node that contains
     * <code>corpus</code>
     *
     * @param parent The parent of appended node
     * @param corpus The corpus that the new appended node should contain
     */
    @Override
    public void add(DefaultTreeNode<Corpus> parent, Corpus corpus) {
        DefaultTreeNode<Corpus> childNode = new DefaultTreeNode(corpus, new ArrayList<DefaultTreeNode<Corpus>>());
        add(parent, childNode);
    }

    @Override
    public void addToSelected(Corpus data) {
        if (!this.selectedCorpora.containsKey(data.getEndpointUrl())) {
            this.selectedCorpora.put(data.getEndpointUrl(), new HashSet<Corpus>());
        }
        this.selectedCorpora.get(data.getEndpointUrl()).add(data);

    }

    @Override
    public void removeFromSelected(Corpus data) {
        if (this.selectedCorpora.containsKey(data.getEndpointUrl())) {
            this.selectedCorpora.get(data.getEndpointUrl()).remove(data);
        }
    }

    @Override
    public Map<String, Set<Corpus>> getSelectedCorpora() {
        return selectedCorpora;
    }

    private void initCorpusTree() {
        //System.out.println("Initializing tree");
        //System.out.println(cache);
        for (Corpus c : cache.getRootCorpora()) {
            // create node from root corpora
            DefaultTreeNode<Corpus> rootChildNode = new DefaultTreeNode(c, new ArrayList<DefaultTreeNode<Corpus>>());
            
            //System.out.println("Adding children to root node " + c.getEndpointUrl() + " " + c.getHandle());
            // add children to that node
            addChildren(rootChildNode);
            
            //System.out.println("Adding root to tree " + c.getEndpointUrl() + " " + c.getHandle());
            // add to the root of the model
            super.getRoot().getChildren().add(rootChildNode);
        }
    }

    private void addChildren(DefaultTreeNode<Corpus> parentNode) {

        Corpus corpus = parentNode.getData();
        
        List<Corpus> corpusChildren = cache.getChildren(corpus);
        //System.out.println("Getting children of " + corpus.getEndpointUrl() + " " + corpus.getHandle() + "--> " + corpusChildren);
        for (Corpus corpusChild : corpusChildren) {
            DefaultTreeNode<Corpus> child = new DefaultTreeNode(corpusChild, new ArrayList<DefaultTreeNode<Corpus>>());
            parentNode.add(child);
            //System.out.println("Adding child " + corpusChild.getEndpointUrl() + " " + corpusChild.getHandle());
            addChildren(child);
        }
    }
}
