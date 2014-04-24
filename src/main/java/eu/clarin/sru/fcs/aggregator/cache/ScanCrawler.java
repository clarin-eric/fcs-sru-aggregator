package eu.clarin.sru.fcs.aggregator.cache;

import eu.clarin.sru.fcs.aggregator.cache.ScanCache;
import eu.clarin.sru.client.SRUScanRequest;
import eu.clarin.sru.client.SRUScanResponse;
import eu.clarin.sru.client.SRUTerm;
import eu.clarin.sru.client.SRUThreadedClient;
import eu.clarin.sru.fcs.aggregator.sopt.CenterRegistryI;
import eu.clarin.sru.fcs.aggregator.sopt.Corpus;
import eu.clarin.sru.fcs.aggregator.sopt.Endpoint;
import eu.clarin.sru.fcs.aggregator.sopt.InstitutionI;
import eu.clarin.sru.fcs.aggregator.util.SRUCQLscan;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author yanapanchenko
 */
public class ScanCrawler {

    private static final Logger LOGGER = Logger.getLogger(ScanCrawler.class.getName());
    private CenterRegistryI cr;
    private SRUThreadedClient sruScanClient;
    private int maxDepth = 1;
    private EndpointFilter filter = null;

    public ScanCrawler(CenterRegistryI centerRegistry, SRUThreadedClient sruScanClient) {
        cr = centerRegistry;
        this.sruScanClient = sruScanClient;
    }

    public ScanCrawler(CenterRegistryI centerRegistry, SRUThreadedClient sruScanClient, EndpointFilter filter, int maxDepth) {
        this(centerRegistry, sruScanClient);
        this.maxDepth = maxDepth;
        this.filter = filter;
    }

    public ScanCache crawl() {

        ScanCache cache = new ScanCache();

        //TODO remember not responding root corpora and come back to them later... ?
        List<InstitutionI> institutions = cr.getCQLInstitutions();
        //LOGGER.info(institutions.toString());
        for (InstitutionI institution : institutions) {
            cache.addInstitution(institution);
            Iterable<Endpoint> endpoints = institution.getEndpoints();
            if (filter != null) {
                endpoints = filter.filter(endpoints);
            }
            for (Endpoint endp : endpoints) {
                Corpus parentCorpus = null;// i.e. it's root
                addCorpora(sruScanClient, endp.getUrl(), institution, 0, parentCorpus, cache);
            }
        }

        return cache;

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
                    String descr = infoNode.getTextContent().replaceAll("&lt;br/&gt;", " ");
                    descr = descr.replaceAll("<br/>", " ");
                    descr = descr.replaceAll("[\t\n\r ]+", " ");
                    c.setDescription(descr.trim());
                    //String lang = element.getAttributeNS("http://clarin.eu/fcs/1.0/resource-info", "lang");
                    //System.out.println("ATTRIBUTE LANG: " + lang);
                    if ("en".equals(element.getAttribute("xml:lang"))) {
                        enDescription = c.getDescription();
                    }
                }
            }
            // description in Engish has priority
            if (enDescription != null && !enDescription.isEmpty()) {
                c.setDescription(enDescription);
            }
        }
    }

    private void addCorpora(SRUThreadedClient sruScanClient, String endpointUrl,
            InstitutionI institution, int depth, Corpus parentCorpus, ScanCache cache) {
        //System.out.println("Adding Corpora: " + endpointUrl + " " + handle);

        Future<SRUScanResponse> corporaResponse = null;

        depth++;
        if (depth > maxDepth) {
            return;
        }
        try {
            boolean root = false;
            if (parentCorpus == null) {
                root = true;
            }

            SRUScanRequest corporaRequest = new SRUScanRequest(endpointUrl);
            StringBuilder scanClause = new StringBuilder(SRUCQLscan.RESOURCE_PARAMETER);
            scanClause.append("=");
            //String normalizedHandle = normalizeHandle(handle, root);
            String normalizedHandle = normalizeHandle(parentCorpus, root);
            scanClause.append(normalizedHandle);
            corporaRequest.setScanClause(scanClause.toString());
            corporaRequest.setExtraRequestData(SRUCQLscan.RESOURCE_INFO_PARAMETER, "true");
            corporaResponse = sruScanClient.scan(corporaRequest);
            Thread.sleep(5000);
            SRUScanResponse response = corporaResponse.get(600, TimeUnit.SECONDS);
            if (response != null && response.hasTerms()) {
                for (SRUTerm term : response.getTerms()) {
                    Corpus c = new Corpus(institution, endpointUrl);
                    c.setHandle(term.getValue());
                    c.setDisplayName(term.getDisplayTerm());
                    c.setNumberOfRecords(term.getNumberOfRecords());
                    addExtraInfo(c, term);
                    cache.addCorpus(c, root, parentCorpus);
                    addCorpora(sruScanClient, c.getEndpointUrl(), c.getInstitution(),
                            depth, c, cache);
                }
                //} else if () {
                // TODO if diagnistics came back, try simple scan without the 
                // SRUCQLscan.RESOURCE_INFO_PARAMETER
            } else {
                if (root) {
                    // create default root corpus:
                    Corpus c = new Corpus(institution, endpointUrl);
                    cache.addCorpus(c);
                }
            }
        } catch (TimeoutException ex) {
            LOGGER.log(Level.SEVERE, "Timeout scanning corpora {0} at {1} {2} {3}",
                    new String[]{Corpus.ROOT_HANDLE, endpointUrl, ex.getClass().getName(), ex.getMessage()});
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error accessing corpora {0} at {1} {2} {3}",
                    new String[]{Corpus.ROOT_HANDLE, endpointUrl, ex.getClass().getName(), ex.getMessage()});
        } finally {
            if (corporaResponse != null && !corporaResponse.isDone()) {
                corporaResponse.cancel(true);
            }
        }
    }

    private String normalizeHandle(Corpus corpus, boolean root) {
        if (root) {
            return Corpus.ROOT_HANDLE;
        }
        String handle = corpus.getHandle();
        if (Corpus.HANDLE_WITH_SPECIAL_CHARS.matcher(handle).matches()) {
            //resourceValue = "%22" + resourceValue + "%22";
            handle = "\"" + handle + "\"";
        }
        return handle;
    }
}
