package eu.clarin.sru.fcs.aggregator.sopt;

import eu.clarin.sru.client.SRUThreadedClient;
import eu.clarin.sru.fcs.aggregator.app.WebAppListener;
import eu.clarin.sru.fcs.aggregator.cache.ScanCrawler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.DefaultTreeModel;
import org.zkoss.zul.DefaultTreeNode;
import org.zkoss.zul.Treeitem;

/**
 * Represents corpora model where corpora data is taken from live requests to 
 * corpus endpoints.
 * 
 * @author Yana Panchenko
 */
public class CorpusModelLive extends DefaultTreeModel<Corpus> implements CorpusModelI {

    private static final Logger logger = Logger.getLogger(CorpusModelLive.class.getName());
    private SRUThreadedClient sruClient;
    private Map<String, Set<Corpus>> selectedCorpora = new HashMap<String, Set<Corpus>>();

    public CorpusModelLive(CenterRegistryI startingPoint) {
        super(new DefaultTreeNode(new Corpus(), new ArrayList<DefaultTreeNode<Corpus>>()));
        sruClient = (SRUThreadedClient) Executions.getCurrent().getDesktop().getWebApp().getAttribute(WebAppListener.SHARED_SRU_CLIENT);
        initRootChildren(startingPoint);
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

    public void loadChildren(Treeitem item) {
         logger.info("LOADING CHILDREN");
        // get first child
        Treeitem childTreeitem = (Treeitem) item.getTreechildren().getChildren().get(0);
        // if this first child is temp node remove it
        DefaultTreeNode<Corpus> openedNodeValue = (DefaultTreeNode<Corpus>) item.getValue();
        Corpus openedCorpus = openedNodeValue.getData();
        DefaultTreeNode<Corpus> childNodeValue = (DefaultTreeNode<Corpus>) childTreeitem.getValue();
        Corpus childCorpus = childNodeValue.getData();
        if (childCorpus.isTemporary()) {
            logger.info("REPLACING TEMP WITH REAL");
            // remove temporary node
            remove(openedNodeValue, childTreeitem.getIndex());
            // add real corpora nodes
            Iterable<Corpus> subcorpora = getSubcorpora(openedCorpus);
            for (Corpus corpus : subcorpora) {
                add(openedNodeValue, corpus);
            }
        }
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
        DefaultTreeNode<Corpus> childNode = createNodeWithTempChildren(corpus);
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

    private void initRootChildren(CenterRegistryI startingPoint) {
        logger.info("INITIALIZING ROOT CHILDREN");
        for (Institution instit : startingPoint.getCQLInstitutions()) {
            for (Endpoint endp : instit.getEndpoints()) {
                    List<Corpus> rootCorpora = ScanCrawler.doScan(sruClient, endp.getUrl(), instit, null);
                    for (Corpus c : rootCorpora) {
                        DefaultTreeNode<Corpus> rootChild = createNodeWithTempChildren(c);
                        super.getRoot().add(rootChild);
                    }
            }

        }
    }
    
    private Iterable<Corpus> getSubcorpora(Corpus corpus) {
        
        return ScanCrawler.doScan(sruClient, corpus.getEndpointUrl(), corpus.getInstitution(), corpus);
    }

    private DefaultTreeNode<Corpus> createNodeWithTempChildren(Corpus corpus) {
        List<DefaultTreeNode<Corpus>> tempChildChildren = new ArrayList<DefaultTreeNode<Corpus>>(1);
        Corpus tempChildCorpus = new Corpus();
        tempChildChildren.add(new DefaultTreeNode<Corpus>(tempChildCorpus));
        DefaultTreeNode<Corpus> child = new DefaultTreeNode<Corpus>(corpus, tempChildChildren);
        return child;
    }
}
