package eu.clarin.sru.fcs.aggregator.app;

import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUScanRequest;
import eu.clarin.sru.client.SRUScanResponse;
import eu.clarin.sru.client.SRUTerm;
import eu.clarin.sru.client.SRUThreadedClient;
import eu.clarin.sru.fcs.aggregator.sopt.CenterRegistryI;
import eu.clarin.sru.fcs.aggregator.sopt.CenterRegistryLive;
import eu.clarin.sru.fcs.aggregator.sopt.Corpus;
import static eu.clarin.sru.fcs.aggregator.sopt.Corpus.ROOT_HANDLE;
import eu.clarin.sru.fcs.aggregator.sopt.CorpusCache;
import eu.clarin.sru.fcs.aggregator.sopt.CorpusModelLive;
import eu.clarin.sru.fcs.aggregator.sopt.Endpoint;
import eu.clarin.sru.fcs.aggregator.sopt.InstitutionI;
import eu.clarin.sru.fcs.aggregator.sopt.Languages;
import eu.clarin.sru.fcs.aggregator.util.SRUCQLscan;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.DefaultTreeNode;

/**
 *
 * @author Yana Panchenko
 */
public class CacheCorporaScanTask extends TimerTask {
    
    private static final Logger logger = Logger.getLogger(CacheCorporaScanTask.class.getName());
    private SRUThreadedClient sruClient;
    private CorpusCache cache;
    
    public CacheCorporaScanTask(CorpusCache cache, SRUThreadedClient sruClient) {
        this.cache = cache;
        this.sruClient = sruClient;
    }

        @Override
        public void run() {
            
            // if previous cahcing is finished??? 
            
            // start caching data into a special temp object
            //CorpusCache tempCache = new CorpusCache();
            LocalTime date = new LocalTime();
            logger.log(Level.INFO, "STARTING CACHING CORPORA SCAN: {0}", new String[]{date.toString()});
            
            Map<String,List<Corpus>> enpUrlToRootCorpora = new HashMap<String,List<Corpus>>(30);
    //Map<String,Set<Corpus>> enpUrlToCorpora = new HashMap<String,Set<Corpus>>();
    Map<Corpus,List<Corpus>> corpusToChildren = new HashMap<Corpus,List<Corpus>>();
    Map<String,Set<Corpus>> langToCorpora = new HashMap<String,Set<Corpus>>();
            CenterRegistryI cr = new CenterRegistryLive();
            List<InstitutionI> institutions = cr.getCQLInstitutions();
            for (InstitutionI institution : institutions) {
                //TODO scan endpoints...
                //and add into cahce
                for (Endpoint endp : institution.getEndpoints()) {
                try {
                    //TODO: temp for testing, this 3 lines are to be removed:
                    //if (!endp.getUrl().contains("uni-leipzig.de")
                    //        && 
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
                    SRUScanResponse response = corporaResponse.get(600, TimeUnit.SECONDS);
                    enpUrlToRootCorpora.put(endp.getUrl(), new ArrayList<Corpus>());
                    if (response != null && response.hasTerms()) {
                        for (SRUTerm term : response.getTerms()) {
                            Corpus c = new Corpus(institution, endp.getUrl());
                            c.setHandle(term.getValue());
                            c.setDisplayName(term.getDisplayTerm());
                            c.setNumberOfRecords(term.getNumberOfRecords());
                            addExtraInfo(c, term);
                            //DefaultTreeNode<Corpus> rootChild = createNodeWithTempChildren(c);
                            //super.getRoot().add(rootChild);
                            enpUrlToRootCorpora.get(endp.getUrl()).add(c);
                            //logger.log(Level.INFO, "Found {0} root corpus for {1}", new String[]{c.getHandle(), endp.getUrl()});
                            for (String lang : c.getLanguages()) {
                                if (!langToCorpora.containsKey(lang)) {
                                    langToCorpora.put(lang, new HashSet<Corpus>());
                                }
                                langToCorpora.get(lang).add(c);
                            }
                            //System.out.println("Adding children to root corpus: " + c);
                            addChildren(c, corpusToChildren);
                        }
                    } else {
                        Corpus endpCorpus = new Corpus(endp.getInstitution(), endp.getUrl());
                        //DefaultTreeNode<Corpus> rootChild = createNodeWithTempChildren(endpCorpus);
                        //super.getRoot().add(rootChild);
                        enpUrlToRootCorpora.get(endp.getUrl()).add(endpCorpus);
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
            //TODO log problems and time taken
            date = new LocalTime();
            logger.log(Level.INFO, "FINISHED CACHING CORPORA SCAN: {0}", new String[]{date.toString()});
            // when finished, replace a shared chache object
            //CorpusCache cache = (CorpusCache) Executions.getCurrent().getDesktop().getWebApp().getAttribute(WebAppListener.CORPUS_CACHE);
            cache.update(enpUrlToRootCorpora, corpusToChildren, langToCorpora);
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
    
        private void addChildren(Corpus corpus, Map<Corpus,List<Corpus>> corpusToChildren) {

            if (corpusToChildren.containsKey(corpus)) {
                return;
            }
         corpusToChildren.put(corpus, new ArrayList<Corpus>());
         
        try {
            SRUScanRequest corporaRequest = new SRUScanRequest(corpus.getEndpointUrl());
            StringBuilder scanClause = new StringBuilder(SRUCQLscan.RESOURCE_PARAMETER);
            scanClause.append("=");
            String resourceValue = corpus.getHandle();
//            if (corpus.getHandle() == null) {
//                resourceValue = ROOT_HANDLE;
//            }
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
                    //addExtraInfo(c, term);
                    addChildren(c, corpusToChildren);
                    corpusToChildren.get(corpus).add(c);
                    //System.out.println("2 Added to : " + corpus + " child: " + c);
                    //System.out.println("2 coprusToChildren changed: " + corpusToChildren);
                    //logger.log(Level.INFO, "Found {0} child corpus for {1} {2}", new String[]{c.getHandle(), c.getEndpointUrl(), corpus.getHandle()});
                    //TODO: temp to finish the caching sooner...
                    //break;
                }
                //logger.log(Level.INFO, "Found {0} children corpora for {1} {2}", new String[]{"" + corpusToChildren.get(corpus).size(), corpus.getEndpointUrl(), corpus.getHandle()});
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

    }

}
