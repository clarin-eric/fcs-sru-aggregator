package eu.clarin.sru.fcs.aggregator.sopt;

import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUScanRequest;
import eu.clarin.sru.client.SRUScanResponse;
import eu.clarin.sru.client.SRUTerm;
import eu.clarin.sru.client.SRUThreadedClient;
import eu.clarin.sru.fcs.aggregator.app.WebAppListener;
import static eu.clarin.sru.fcs.aggregator.sopt.Corpus.ROOT_HANDLE;
import eu.clarin.sru.fcs.aggregator.util.SRUCQLscan;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.DefaultTreeModel;
import org.zkoss.zul.DefaultTreeNode;
import org.zkoss.zul.Treeitem;

/**
 *
 * @author Yana Panchenko
 */
public class CorpusLiveModel extends DefaultTreeModel<Corpus> {

    private static final Logger logger = Logger.getLogger(CorpusLiveModel.class.getName());
    private SRUThreadedClient sruClient;
    private Map<String, Set<Corpus>> selectedCorpora = new HashMap<String, Set<Corpus>>();

    public CorpusLiveModel(StartingPointFCS startingPoint) {
        super(new DefaultTreeNode(new Corpus(), new ArrayList<DefaultTreeNode<Corpus>>()));
        sruClient = (SRUThreadedClient) Executions.getCurrent().getDesktop().getWebApp().getAttribute(WebAppListener.SHARED_SRU_CLIENT);
        initRootChildren(startingPoint);
    }

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
    public void add(DefaultTreeNode<Corpus> parent, DefaultTreeNode<Corpus> newNode) {
        parent.getChildren().add(newNode);
    }

    public boolean hasChildren(Treeitem item) {
        return !item.getTreechildren().getChildren().isEmpty();
    }

    public void loadChildren(Treeitem item) {
        // get first child
        Treeitem childTreeitem = (Treeitem) item.getTreechildren().getChildren().get(0);
        // if this first child is temp node remove it
        DefaultTreeNode<Corpus> openedNodeValue = (DefaultTreeNode<Corpus>) item.getValue();
        Corpus openedCorpus = openedNodeValue.getData();
        DefaultTreeNode<Corpus> childNodeValue = (DefaultTreeNode<Corpus>) childTreeitem.getValue();
        Corpus childCorpus = childNodeValue.getData();
        if (childCorpus.isTemporary()) {
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
    public void add(DefaultTreeNode<Corpus> parent, Corpus corpus) {
        DefaultTreeNode<Corpus> childNode = createNodeWithTempChildren(corpus);
        add(parent, childNode);
    }

    public void addToSelected(Corpus data) {
        if (!this.selectedCorpora.containsKey(data.getEndpointUrl())) {
            this.selectedCorpora.put(data.getEndpointUrl(), new HashSet<Corpus>());
        }
        this.selectedCorpora.get(data.getEndpointUrl()).add(data);

    }

    public void removeFromSelected(Corpus data) {
        if (this.selectedCorpora.containsKey(data.getEndpointUrl())) {
            this.selectedCorpora.get(data.getEndpointUrl()).remove(data);
        }
    }

    public Map<String, Set<Corpus>> getSelectedCorpora() {
        return selectedCorpora;
    }

    private void initRootChildren(StartingPointFCS startingPoint) {
        for (Institution instit : startingPoint.getInstitutions()) {
            for (Endpoint endp : instit.getChildren()) {
                try {
                    //TODO: temp for testing, this 3 lines are to be removed:
                    //if (//!endp.getUrl().contains("uni-leipzig.de") && 
                    //        !endp.getUrl().contains("mpi.")
                    //        && !endp.getUrl().contains("ids-mannheim")
                    //        && !endp.getUrl().contains("weblicht")) {
                    //    continue;
                    //}

                    Future<SRUScanResponse> corporaResponse = null;
                    SRUScanRequest corporaRequest = new SRUScanRequest(endp.getUrl());
                    StringBuilder scanClause = new StringBuilder(SRUCQLscan.RESOURCE_PARAMETER);
                    scanClause.append("=");
                    scanClause.append(ROOT_HANDLE);
                    corporaRequest.setScanClause(scanClause.toString());
                    corporaRequest.setExtraRequestData(SRUCQLscan.RESOURCE_INFO_PARAMETER, "true");
                    corporaResponse = sruClient.scan(corporaRequest);
                    SRUScanResponse response = corporaResponse.get(200, TimeUnit.SECONDS);
                    if (response != null && response.hasTerms()) {
                        for (SRUTerm term : response.getTerms()) {
                            Corpus c = new Corpus(instit, endp.getUrl());
                            c.setHandle(term.getValue());
                            c.setDisplayName(term.getDisplayTerm());
                            c.setNumberOfRecords(term.getNumberOfRecords());
                            addExtraInfo(c, term);
                            DefaultTreeNode<Corpus> rootChild = createNodeWithTempChildren(c);
                            super.getRoot().add(rootChild);
                        }
                    } else {
                        Corpus endpCorpus = new Corpus(endp.getInstitution(), endp.getUrl());
                        DefaultTreeNode<Corpus> rootChild = createNodeWithTempChildren(endpCorpus);
                        super.getRoot().add(rootChild);
                    }
                } catch (SRUClientException ex) {
                    logger.log(Level.SEVERE, "Error accessing corpora {0} at {1} {2} {3}",
                            new String[]{ROOT_HANDLE, endp.getUrl(), ex.getClass().getName(), ex.getMessage()});
                } catch (InterruptedException ex) {
                    logger.log(Level.SEVERE, "Error accessing corpora {0} at {1} {2} {3}",
                            new String[]{ROOT_HANDLE, endp.getUrl(), ex.getClass().getName(), ex.getMessage()});
                } catch (ExecutionException ex) {
                    logger.log(Level.SEVERE, "Error accessing corpora {0} at {1} {2} {3}",
                            new String[]{ROOT_HANDLE, endp.getUrl(), ex.getClass().getName(), ex.getMessage()});
                } catch (TimeoutException ex) {
                    logger.log(Level.SEVERE, "Timeout scanning corpora {0} at {1} {2} {3}",
                            new String[]{ROOT_HANDLE, endp.getUrl(), ex.getClass().getName(), ex.getMessage()});
                }
            }

        }
    }

    private Iterable<Corpus> getSubcorpora(Corpus corpus) {

        ArrayList<Corpus> subCorpora = new ArrayList<Corpus>();
        try {
            SRUScanRequest corporaRequest = new SRUScanRequest(corpus.getEndpointUrl());
            StringBuilder scanClause = new StringBuilder(SRUCQLscan.RESOURCE_PARAMETER);
            scanClause.append("=");
            String resourceValue = corpus.getHandle();
            if (corpus.getHandle() == null) {
                resourceValue = ROOT_HANDLE;
            }
            if (Corpus.HANDLE_WITH_SPECIAL_CHARS.matcher(resourceValue).matches()) {
                resourceValue = "%22" + resourceValue + "%22";
            }
            scanClause.append(resourceValue);
            corporaRequest.setScanClause(scanClause.toString());
            //!!!TODO request doesn't work for scan with resource handle???
            //corporaRequest.setExtraRequestData(SRUCQLscan.RESOURCE_INFO_PARAMETER, "true");
            Future<SRUScanResponse> corporaResponse = sruClient.scan(corporaRequest);
            SRUScanResponse response = corporaResponse.get(200, TimeUnit.SECONDS);
            if (response != null && response.hasTerms()) {
            for (SRUTerm term : response.getTerms()) {
                Corpus c = new Corpus(corpus.getInstitution(), corpus.getEndpointUrl());
                c.setHandle(term.getValue());
                c.setDisplayName(term.getDisplayTerm());
                c.setNumberOfRecords(term.getNumberOfRecords());
                addExtraInfo(c, term);
                subCorpora.add(c);
            }
            System.out.println("Found " + subCorpora.size() + " children");
        }
        } catch (SRUClientException ex) {
            logger.log(Level.SEVERE, "Error accessing corpora {0} at {1} {2} {3}",
                    new String[]{corpus.getHandle(), corpus.getEndpointUrl(), ex.getClass().getName(), ex.getMessage()});
        } catch (InterruptedException ex) {
            logger.log(Level.SEVERE, "Error accessing corpora {0} at {1} {2} {3}",
                    new String[]{corpus.getHandle(), corpus.getEndpointUrl(), ex.getClass().getName(), ex.getMessage()});
        } catch (ExecutionException ex) {
            logger.log(Level.SEVERE, "Error accessing corpora {0} at {1} {2} {3}",
                    new String[]{corpus.getHandle(), corpus.getEndpointUrl(), ex.getClass().getName(), ex.getMessage()});
        } catch (TimeoutException ex) {
            logger.log(Level.SEVERE, "Timeout scanning corpora {0} at {1} {2} {3}",
                    new String[]{corpus.getHandle(), corpus.getEndpointUrl(), ex.getClass().getName(), ex.getMessage()});
        }

        return subCorpora;
    }

    private void addExtraInfo(Corpus c, SRUTerm term) {

        DocumentFragment extraInfo = term.getExtraTermData();
        String enDescription = null;
        if (extraInfo != null) {
            NodeList infoNodes = extraInfo.getChildNodes().item(0).getChildNodes();
            for (int i = 0; i < infoNodes.getLength(); i++) {
                Node infoNode = infoNodes.item(i);
                if (infoNode.getNodeType() == Node.ELEMENT_NODE && infoNode.getLocalName().equals("LandingPageURI")) {
                    c.setLandingPage(infoNode.getTextContent().trim());
                } else if (infoNode.getNodeType() == Node.ELEMENT_NODE && infoNode.getLocalName().equals("Languages")) {
                    NodeList languageNodes = infoNode.getChildNodes();
                    for (int j = 0; j < languageNodes.getLength(); j++) {
                        if (languageNodes.item(j).getNodeType() == Node.ELEMENT_NODE && languageNodes.item(j).getLocalName().equals("Language")) {
                            Element languageNode = (Element) languageNodes.item(j);
                            String languageText = languageNode.getTextContent().trim();
                            if (!languageText.isEmpty()) {
                                c.addLanguage(languageText.trim());
                            }
                        }

                    }
                } else if (infoNode.getNodeType() == Node.ELEMENT_NODE && infoNode.getLocalName().equals("Description")) {
                    Element element = (Element) infoNode;
                    c.setDescription(infoNode.getTextContent().trim());
                    //String lang = element.getAttributeNS("http://clarin.eu/fcs/1.0/resource-info", "lang");
                    //System.out.println("ATTRIBUTE LANG: " + lang);
                    if ("en".equals(element.getAttribute("xml:lang"))) {
                        enDescription = infoNode.getTextContent().trim();
                    }
                }
            }
            // description in Engish has priority
            if (enDescription != null && !enDescription.isEmpty()) {
                c.setDescription(enDescription);
            }
        }
    }

    private DefaultTreeNode<Corpus> createNodeWithTempChildren(Corpus corpus) {
        List<DefaultTreeNode<Corpus>> tempChildChildren = new ArrayList<DefaultTreeNode<Corpus>>(1);
        Corpus tempChildCorpus = new Corpus();
        tempChildChildren.add(new DefaultTreeNode<Corpus>(tempChildCorpus));
        DefaultTreeNode<Corpus> child = new DefaultTreeNode<Corpus>(corpus, tempChildChildren);
        return child;
    }
}
