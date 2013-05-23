/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.clarin.sru.fcs.aggregator.sparam2;

import eu.clarin.sru.client.SRUClient;
import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUScanRequest;
import eu.clarin.sru.client.SRUScanResponse;
import eu.clarin.sru.client.SRUTerm;
import eu.clarin.sru.client.SRUVersion;
import eu.clarin.sru.fcs.aggregator.data.CenterRegistry;
import eu.clarin.sru.fcs.aggregator.data.Corpus;
import eu.clarin.sru.fcs.aggregator.data.Endpoint;
import eu.clarin.sru.fcs.aggregator.data.Institution;
import eu.clarin.sru.fcs.aggregator.sparam.CorpusTreeNode;
import eu.clarin.sru.fcs.aggregator.util.SRUCQLscan;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.zkoss.zul.DefaultTreeNode;

/**
 *
 * @author Yana Panchenko <yana_panchenko at yahoo.com>
 */
public class Corpus2 {

    private static final Logger logger = Logger.getLogger(Corpus.class.getName());
    public static final String ROOT_HANDLE = "root";
    private static final Pattern HANDLE_WITH_SPECIAL_CHARS = Pattern.compile(".*[<>=/()\\s].*");

    
    private Institution institution;
    private String endpointUrl;
    private String handle;
    private Integer numberOfRecords;
    private String displayTerm;
    private Set<String> languages = new HashSet<String>();
    private String landingPage;
    private String title;
    private String description;
    boolean temp = false;

    

    public Corpus2() {
        temp = true;
    }

    public Corpus2(Institution institution, String endpointUrl) {
        this.institution = institution;
        this.endpointUrl = endpointUrl;
    }

//    public boolean isRoot() {
//        if (!temp && handle == null) {
//            return true;
//        }
//        return false;
//    }

    public boolean isTemporary() {
        return temp;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String value) {
        this.handle = value;
    }

    public void setNumberOfRecords(int numberOfRecords) {
        this.numberOfRecords = numberOfRecords;
    }

    public Integer getNumberOfRecords() {
        return numberOfRecords;
    }

    public String getDisplayName() {
        return displayTerm;
    }

    public void setDisplayName(String displayName) {
        this.displayTerm = displayName;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public Institution getInstitution() {
        return institution;
    }

    public Set<String> getLanguages() {
        return languages;
    }

    public void setLanguages(Set<String> languages) {
        this.languages = languages;
    }

    public void addLanguage(String language) {
        this.languages.add(language);
    }

    public String getLandingPage() {
        return landingPage;
    }

    public void setLandingPage(String landingPage) {
        this.landingPage = landingPage;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    public static List<DefaultTreeNode<Corpus2>> initCorpusChildren(CenterRegistry registry) {

        List<DefaultTreeNode<Corpus2>> rootChildren = new ArrayList<DefaultTreeNode<Corpus2>>();
        for (CorpusTreeNode regChild : registry.getChildren()) {
            Institution instit = (Institution) regChild;
            for (CorpusTreeNode institChild : instit.getChildren()) {
                Endpoint endp = (Endpoint) institChild;
                
                //TODO: temp for testing, this 3 lines are to be removed:
                //if (!endp.getUrl().startsWith("http://cqlservlet.mpi.nl") && !endp.getUrl().contains("weblicht")) {
                //    continue;
                //}
                
                SRUScanResponse corporaResponse = null;
                try {
                    SRUClient sruClient = new SRUClient(SRUVersion.VERSION_1_2);
                    SRUScanRequest corporaRequest = new SRUScanRequest(endp.getUrl());
                    StringBuilder scanClause = new StringBuilder(SRUCQLscan.RESOURCE_PARAMETER);
                    scanClause.append("=");
                    scanClause.append(ROOT_HANDLE);
                    corporaRequest.setScanClause(scanClause.toString());
                    corporaRequest.setExtraRequestData("x-cmd-resource-info", "true");
                    corporaResponse = sruClient.scan(corporaRequest);
                } catch (SRUClientException ex) {
                    logger.log(Level.SEVERE, "Error accessing corpora {0} at {1} {2} {3}",
                    new String[]{ROOT_HANDLE, endp.getUrl(), ex.getClass().getName(), ex.getMessage()});
                }
                if (corporaResponse != null && corporaResponse.hasTerms()) {
                    for (SRUTerm term : corporaResponse.getTerms()) {
                        Corpus2 c = new Corpus2(instit, endp.getUrl());
                        c.setHandle(term.getValue());
                        c.setDisplayName(term.getDisplayTerm());
                        c.setNumberOfRecords(term.getNumberOfRecords());
                        addExtraInfo(c, term);

                        List<DefaultTreeNode<Corpus2>> tempChildChildren = new ArrayList<DefaultTreeNode<Corpus2>>(1);
                        Corpus2 tempChildCorpus = new Corpus2();
                        tempChildChildren.add(new DefaultTreeNode<Corpus2>(tempChildCorpus));
                        DefaultTreeNode<Corpus2> rootChild = new DefaultTreeNode<Corpus2>(c, tempChildChildren);
                        rootChildren.add(rootChild);
                    }
                } else {
                    Corpus2 endpCorpus = new Corpus2(endp.getInstitution(), endp.getUrl());
                    List<DefaultTreeNode<Corpus2>> tempChildChildren = new ArrayList<DefaultTreeNode<Corpus2>>(1);
                    Corpus2 tempChildCorpus = new Corpus2();
                    tempChildChildren.add(new DefaultTreeNode<Corpus2>(tempChildCorpus));
                    DefaultTreeNode<Corpus2> rootChild = new DefaultTreeNode<Corpus2>(endpCorpus, tempChildChildren);
                    rootChildren.add(rootChild);
                }
            }
            
        }
        return rootChildren;
    }

        public static Iterable<Corpus2> getSubcorpora(Corpus2 corpus) {


        ArrayList<Corpus2> subCorpora = new ArrayList<Corpus2>();
        SRUScanResponse corporaResponse = null;

        try {
            SRUClient sruClient = new SRUClient(SRUVersion.VERSION_1_2);
            SRUScanRequest corporaRequest = new SRUScanRequest(corpus.getEndpointUrl());
            StringBuilder scanClause = new StringBuilder(SRUCQLscan.RESOURCE_PARAMETER);
            scanClause.append("=");
            String resourceValue = corpus.getHandle();
            if (corpus.getHandle() == null) {
                resourceValue = ROOT_HANDLE;
            }
            if (HANDLE_WITH_SPECIAL_CHARS.matcher(resourceValue).matches()) {
                resourceValue = "%22" + resourceValue + "%22";
            }
            scanClause.append(resourceValue);
            corporaRequest.setScanClause(scanClause.toString());
            //TODO extra data?
            //corporaRequest.setExtraRequestData("x-cmd-resource-info", "true");
            corporaResponse = sruClient.scan(corporaRequest);
        } catch (SRUClientException ex) {
            logger.log(Level.SEVERE, "Error accessing corpora {0} at {1} {2} {3}",
                    new String[]{corpus.getHandle(), corpus.getEndpointUrl(), ex.getClass().getName(), ex.getMessage()});
        }


        if (corporaResponse != null && corporaResponse.hasTerms()) {
            for (SRUTerm term : corporaResponse.getTerms()) {
                Corpus2 c = new Corpus2(corpus.getInstitution(), corpus.getEndpointUrl());
                c.setHandle(term.getValue());
                c.setDisplayName(term.getDisplayTerm());
                c.setNumberOfRecords(term.getNumberOfRecords());
                addExtraInfo(c, term);
                subCorpora.add(c);
            }
            System.out.println("Found " + subCorpora.size() + " children");
        }

        return subCorpora;
    }
        
        private static void addExtraInfo(Corpus2 c, SRUTerm term) {
                        
                DocumentFragment extraInfo = term.getExtraTermData();
                if (extraInfo != null) {
                NodeList infoNodes = extraInfo.getChildNodes().item(0).getChildNodes();
                for (int i = 0; i < infoNodes.getLength(); i++) {
                    Node infoNode = infoNodes.item(i);
                    if (infoNode.getNodeName().equals("LandingPageURI")) {
                        c.setLandingPage(infoNode.getTextContent().trim());
                    } else if (infoNode.getNodeName().equals("Languages")) {
                        for (int j = 0; j < infoNode.getChildNodes().getLength(); j++) {
                            String languageText = infoNode.getChildNodes().item(j).getTextContent();
                            if (!languageText.isEmpty()) {
                                c.addLanguage(languageText.trim());
                            }
                        }
                    } else if (infoNode.getNodeName().equals("Description")) {
                        //c.setDescription("This is the corpus of newspapers from the years 1995-1998, covering a wide range of topics, from economics and politics, to sports and cultural events.");
                        //TODO: select only with the xml:lang=en
                        c.setDescription(infoNode.getTextContent().trim());
                    }
                }
                
                }
    }
}
