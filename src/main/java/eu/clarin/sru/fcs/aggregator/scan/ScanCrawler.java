package eu.clarin.sru.fcs.aggregator.scan;

import eu.clarin.sru.fcs.aggregator.util.CounterLatch;
import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUDiagnostic;
import eu.clarin.sru.client.SRUExplainRequest;
import eu.clarin.sru.client.SRUExplainResponse;
import eu.clarin.sru.client.SRUExtraResponseData;
import eu.clarin.sru.client.SRUScanRequest;
import eu.clarin.sru.client.SRUScanResponse;
import eu.clarin.sru.client.SRUTerm;
import eu.clarin.sru.client.fcs.ClarinFCSEndpointDescription;
import eu.clarin.sru.client.fcs.ClarinFCSEndpointDescription.ResourceInfo;
import eu.clarin.sru.fcs.aggregator.client.ThrottledClient;
import eu.clarin.sru.fcs.aggregator.util.SRUCQL;
import eu.clarin.sru.fcs.aggregator.util.Throw;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Crawler for collecting endpoint scan operation responses of FCS
 * specification. Collects all the endpoints and resources descriptions.
 *
 * @author yanapanchenko
 * @author edima
 * @author ljo
 */
public class ScanCrawler {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ScanCrawler.class);

    private final List<Institution> institutions;
    private final int maxDepth;
    private final CounterLatch latch;
    private final ThrottledClient sruClient;
    private final Statistics statistics = new Statistics();

    private static final URI advSearchCap;
    static {
        try {
            advSearchCap = new URI("http://clarin.eu/fcs/capability/advanced-search");
        } catch (URISyntaxException e) {
            throw new RuntimeException("Class initialization failed due to static variable exception.", e);
        }
    }

    public ScanCrawler(List<Institution> institutions, ThrottledClient sruClient, int maxDepth) {
        this.institutions = institutions;
        this.sruClient = sruClient;
        this.maxDepth = maxDepth;
        this.latch = new CounterLatch();
    }

    public Resources crawl() {
        Resources cache = new Resources();
        for (Institution institution : institutions) {
            cache.addInstitution(institution);
            Iterable<Endpoint> endpoints = institution.getEndpoints();
            for (Endpoint endp : endpoints) {
                new ExplainTask(institution, endp, cache).start();
            }
        }

        try {
            log.info("awaiting");
            latch.await();
        } catch (InterruptedException e) {
            log.error("INTERRUPTED wait for {} scan task(s), are we shutting down?", latch.get());
        }

        log.info("done crawling");

        for (Institution institution : institutions) {
            Iterable<Endpoint> endpoints = institution.getEndpoints();
            for (Endpoint endp : endpoints) {
                log.info("inst/endpoint type: {} / {}", institution.getName(), endp.getProtocol());
            }
        }

        return cache;
    }

    class ExplainTask implements ThrottledClient.ExplainCallback {

        final Institution institution;
        final Endpoint endpoint;
        final Resources resources;

        ExplainTask(final Institution institution, final Endpoint endpoint, final Resources resources) {
            this.institution = institution;
            this.endpoint = endpoint;
            this.resources = resources;
        }

        void start() {
            SRUExplainRequest explainRequest = null;
            try {
                statistics.initEndpoint(institution, endpoint,
                        sruClient.getMaxConcurrentRequests(false, endpoint.getUrl()));
                explainRequest = new SRUExplainRequest(endpoint.getUrl());
                explainRequest.setExtraRequestData(SRUCQL.EXPLAIN_ASK_FOR_RESOURCES_PARAM, "true");
                explainRequest.setParseRecordDataEnabled(true);
            } catch (Throwable ex) {
                log.error("Exception creating explain request for {}: {}", endpoint.getUrl(), ex.getMessage());
                log.error("--> ", ex);
            }
            if (explainRequest == null) {
                return;
            }

            log.info("{} Start explain: {}", latch.get(), endpoint.getUrl());
            latch.increment();
            sruClient.explain(explainRequest, this);
        }

        @Override
        public void onSuccess(SRUExplainResponse response, ThrottledClient.Stats stats) {
            try {
                statistics.addEndpointDatapoint(institution, endpoint, stats.getQueueTime(), stats.getExecutionTime());

                List<String> rootResources = new ArrayList<String>();
                if (response != null && response.hasExtraResponseData()) {
                    for (SRUExtraResponseData data : response.getExtraResponseData()) {
                        if (data instanceof ClarinFCSEndpointDescription) {

                            ClarinFCSEndpointDescription desc = (ClarinFCSEndpointDescription) data;
                            if (desc.getVersion() == 2) {
                                endpoint.setProtocol(FCSProtocolVersion.VERSION_2);
                                if (desc.getCapabilities().contains(advSearchCap)) {
                                    endpoint.addSearchCapability(FCSSearchCapabilities.ADVANCED_SEARCH);
                                }
                            } else {
                                endpoint.setProtocol(FCSProtocolVersion.VERSION_1);
                                // endpoint.addSearchCapability(FCSSearchCapabilities.BASIC_SEARCH);
                            }
                            statistics.upgradeProtocolVersion(institution, endpoint);

                            addResources(resources, institution, endpoint, rootResources, desc.getResources(), null);
                        }
                    }
                }
                statistics.addEndpointResources(institution, endpoint, rootResources);

                if (response != null && response.hasDiagnostics()) {
                    for (SRUDiagnostic d : response.getDiagnostics()) {
                        SRUExplainRequest request = response.getRequest();
                        Diagnostic diag = new Diagnostic(d.getURI(), d.getMessage(), d.getDetails());
                        statistics.addEndpointDiagnostic(institution, endpoint, diag,
                                response.getRequest().getRequestedURI().toString());
                        log.info("Diagnostic: {}: {}", response.getRequest().getRequestedURI().toString(),
                                diag.message);
                    }
                }

                log.info("{} Finished explain, endpoint version is {}: {}", latch.get(), endpoint.getProtocol(),
                        endpoint.getUrl());
            } catch (Exception xc) {
                log.error("{} Exception in explain callback {}", latch.get(), endpoint.getUrl());
                log.error("--> ", xc);
                statistics.addErrorDatapoint(institution, endpoint, xc,
                        response.getRequest().getRequestedURI().toString());
            } finally {
                if (endpoint.getProtocol().equals(FCSProtocolVersion.LEGACY)) {
                    new ScanTask(institution, endpoint, null, resources, 0).start();
                    Diagnostic diag = new Diagnostic("LEGACY",
                            "Endpoint didn't return any resource on EXPLAIN, presuming legacy support", "");
                    statistics.addEndpointDiagnostic(institution, endpoint, diag,
                            response.getRequest().getRequestedURI().toString());
                }

                latch.decrement();
            }
        }

        @Override
        public void onError(SRUExplainRequest request, SRUClientException error, ThrottledClient.Stats stats) {
            try {
                log.error("{} Error while explaining {}: {}", latch.get(), endpoint.getUrl(), error.getMessage());
                statistics.addEndpointDatapoint(institution, endpoint, stats.getQueueTime(), stats.getExecutionTime());
                statistics.addErrorDatapoint(institution, endpoint, error, request.getRequestedURI().toString());
                if (Throw.isCausedBy(error, SocketTimeoutException.class)) {
                    return;
                }
                log.error("--> " + request.getBaseURI() + " --> ", error);
            } catch (Throwable xc) {
                log.error("explain.onError exception:", xc);
            } finally {
                latch.decrement();
            }
        }
    }

    private static void addResources(Resources resources,
            Institution institution, Endpoint endpoint,
            List<String> rootResources,
            List<ResourceInfo> resourceInfos, Resource parentResource) {
        if (resourceInfos == null) {
            return;
        }
        for (ResourceInfo ri : resourceInfos) {
            Resource r = new Resource(institution, endpoint);
            r.setHandle(ri.getPid());
            r.setTitle(getBestValueFrom(ri.getTitle()));
            r.setDescription(getBestValueFromNullable(ri.getDescription()));
            r.setInstitution(getBestValueFromNullable(ri.getInstitution(), institution.getName()));
            r.setLandingPage(ri.getLandingPageURI());
            r.setLanguages(new HashSet<String>(ri.getLanguages()));

            r.setSearchCapabilities(endpoint.getSearchCapabilities());
            r.setAvailableDataViews(ri.getAvailableDataViews());
            r.setAvailableLayers(ri.getAvailableLayers());

            if (resources.addResource(r, parentResource)) {
                if (rootResources != null) {
                    rootResources.add(r.getTitle());
                }
                addResources(resources, institution, endpoint, null, ri.getSubResources(), r);
            } else {
                Resource otherResource = resources.findByHandle(r.getHandle());
                if (otherResource != null && endpoint.equals(otherResource.getEndpoint())) {
                    log.warn("Found multiple resources with same handle '{}' at endpoint {}", r.getHandle(),
                            endpoint.getUrl());
                } else {
                    log.warn("Found existing resource with same handle '{}' at endpoint {}. Skip for this endpoint {}.",
                            r.getHandle(), (otherResource != null) ? otherResource.getEndpoint().getUrl() : null,
                            endpoint.getUrl());
                }
            }
        }
    }

    private static String getBestValueFromNullable(Map<String, String> map, String defaultValue) {
        if (map == null || map.isEmpty()) {
            return defaultValue;
        }
        return getBestValueFrom(map);
    }

    private static String getBestValueFromNullable(Map<String, String> map) {
        return getBestValueFromNullable(map, null);
    }

    private static String getBestValueFrom(Map<String, String> map) {
        String ret = map.get("en");
        if (ret == null || ret.trim().isEmpty()) {
            ret = map.get("eng");
        }
        if (ret == null || ret.trim().isEmpty()) {
            ret = map.get(null);
        }
        if (ret == null || ret.trim().isEmpty()) {
            ret = map.get("de");
        }
        if (ret == null || ret.trim().isEmpty()) {
            ret = map.get("deu");
        }
        if (ret == null || ret.trim().isEmpty()) {
            ret = map.size() > 0
                    ? map.values().iterator().next()
                    : null;
        }
        return ret;
    }

    class ScanTask implements ThrottledClient.ScanCallback {

        final Institution institution;
        final Endpoint endpoint;
        final Resource parentResource;
        final Resources resources;
        final int depth;

        ScanTask(final Institution institution, final Endpoint endpoint,
                final Resource parentResource, final Resources resources, final int depth) {
            this.institution = institution;
            this.endpoint = endpoint;
            this.parentResource = parentResource;
            this.resources = resources;
            this.depth = depth;
        }

        private void start() {
            if (depth > maxDepth) {
                return;
            }

            SRUScanRequest scanRequest = null;
            try {
                scanRequest = new SRUScanRequest(endpoint.getUrl());
                scanRequest.setScanClause(SRUCQL.SCAN_RESOURCE_PARAMETER
                        + "=" + normalizeHandle(parentResource));
                scanRequest.setExtraRequestData(SRUCQL.SCAN_RESOURCE_INFO_PARAMETER,
                        SRUCQL.SCAN_RESOURCE_INFO_PARAMETER_DEFAULT_VALUE);
            } catch (Throwable ex) {
                log.error("Exception creating scan request for {}: {}", endpoint.getUrl(), ex.getMessage());
                log.error("--> ", ex);
            }
            if (scanRequest == null) {
                return;
            }

            log.info("{} Start scan: {}#{}", latch.get(), endpoint.getUrl(), normalizeHandle(parentResource));
            latch.increment();
            sruClient.scan(scanRequest, this);
        }

        @Override
        public void onSuccess(SRUScanResponse response, ThrottledClient.Stats stats) {
            try {
                statistics.addEndpointDatapoint(institution, endpoint, stats.getQueueTime(), stats.getExecutionTime());
                if (response != null && response.hasTerms()) {
                    for (SRUTerm term : response.getTerms()) {
                        if (term == null) {
                            log.warn("null term for scan at endpoint {}", endpoint.getUrl());
                        } else {
                            Resource r = createResource(institution, endpoint, term);
                            if (resources.addResource(r, parentResource)) {
                                new ScanTask(institution, endpoint, r, resources, depth + 1).start();
                                if (parentResource == null) {
                                    statistics.addEndpointResource(institution, endpoint, r.getTitle());
                                }
                            }
                        }
                    }
                }

                if (response != null && response.hasDiagnostics()) {
                    for (SRUDiagnostic d : response.getDiagnostics()) {
                        Diagnostic diag = new Diagnostic(d.getURI(), d.getMessage(), d.getDetails());
                        statistics.addEndpointDiagnostic(institution, endpoint, diag,
                                response.getRequest().getRequestedURI().toString());
                        log.info("Diagnostic: {}: {}", response.getRequest().getRequestedURI().toString(),
                                diag.message);
                    }
                }

                log.info("{} Finished scan: {}#{}", latch.get(), endpoint.getUrl(), normalizeHandle(parentResource));
            } catch (Exception xc) {
                log.error("{} Exception in scan callback {}#{}", latch.get(), endpoint.getUrl(),
                        normalizeHandle(parentResource));
                log.error("--> ", xc);
                statistics.addErrorDatapoint(institution, endpoint, xc,
                        response.getRequest().getRequestedURI().toString());
            } finally {
                latch.decrement();
            }
        }

        @Override
        public void onError(SRUScanRequest request, SRUClientException error, ThrottledClient.Stats stats) {
            try {
                log.error("{} Error while scanning {}#{}: {}", latch.get(), endpoint.getUrl(),
                        normalizeHandle(parentResource), error.getMessage());
                statistics.addEndpointDatapoint(institution, endpoint, stats.getQueueTime(), stats.getExecutionTime());
                statistics.addErrorDatapoint(institution, endpoint, error, request.getRequestedURI().toString());
                if (Throw.isCausedBy(error, SocketTimeoutException.class)) {
                    return;
                }
                log.error("--> " + request.getBaseURI() + "?" + request.getScanClause() + " --> ", error);
            } catch (Throwable xc) {
                log.error("scan.onError exception:", xc);
            } finally {
                latch.decrement();
            }
        }
    }

    public Statistics getStatistics() {
        return statistics;
    }

    private static String normalizeHandle(Resource resource) {
        if (resource == null) {
            return Resource.ROOT_HANDLE;
        }
        String handle = resource.getHandle();
        if (Resource.HANDLE_WITH_SPECIAL_CHARS.matcher(handle).matches()) {
            handle = "\"" + handle + "\"";
        }
        return handle;
    }

    private static Resource createResource(Institution institution, Endpoint endpoint, SRUTerm term) {
        Resource r = new Resource(institution, endpoint);
        r.setTitle("" + term.getDisplayTerm());
        r.setInstitution(institution.getName());
        String handle = term.getValue();
        if (handle == null) {
            log.error("null handle for resource: {} : {}", endpoint, term.getDisplayTerm());
            handle = "";
        }
        r.setHandle(handle);
        if (term.getNumberOfRecords() > 0) {
            r.setNumberOfRecords(term.getNumberOfRecords());
        }
        addExtraInfo(r, term);
        return r;
    }

    private static void addExtraInfo(Resource r, SRUTerm term) {
        DocumentFragment extraInfo = term.getExtraTermData();
        String enDescription = null, enTitle = null;
        if (extraInfo != null) {
            NodeList infoNodes = extraInfo.getChildNodes().item(0).getChildNodes();
            for (int i = 0; i < infoNodes.getLength(); i++) {
                Node infoNode = infoNodes.item(i);
                if (infoNode.getNodeType() == Node.ELEMENT_NODE && infoNode.getLocalName().equals("LandingPageURI")) {
                    r.setLandingPage(infoNode.getTextContent().trim());
                } else if (infoNode.getNodeType() == Node.ELEMENT_NODE && infoNode.getLocalName().equals("Languages")) {
                    NodeList languageNodes = infoNode.getChildNodes();
                    for (int j = 0; j < languageNodes.getLength(); j++) {
                        if (languageNodes.item(j).getNodeType() == Node.ELEMENT_NODE
                                && languageNodes.item(j).getLocalName().equals("Language")) {
                            Element languageNode = (Element) languageNodes.item(j);
                            String languageText = languageNode.getTextContent();
                            if (languageText != null && !languageText.trim().isEmpty()) {
                                r.addLanguage(languageText.trim());
                            }
                        }
                    }
                } else if (infoNode.getNodeType() == Node.ELEMENT_NODE && infoNode.getLocalName().equals("Title")) {
                    Element element = (Element) infoNode;
                    String x = cleanup(infoNode.getTextContent());
                    if (!x.isEmpty()) {
                        r.setTitle(x);
                        if ("en".equals(element.getAttribute("xml:lang"))) {
                            enTitle = x;
                        }
                    }
                } else if (infoNode.getNodeType() == Node.ELEMENT_NODE
                        && infoNode.getLocalName().equals("Description")) {
                    Element element = (Element) infoNode;
                    String x = cleanup(infoNode.getTextContent());
                    r.setDescription(x);
                    // String lang =
                    // element.getAttributeNS("http://clarin.eu/fcs/1.0/resource-info", "lang");
                    // System.out.println("ATTRIBUTE LANG: " + lang);
                    if ("en".equals(element.getAttribute("xml:lang"))) {
                        enDescription = x;
                    }
                }
            }
            // title in English has priority
            if (enTitle != null && !enTitle.isEmpty()) {
                r.setTitle(enTitle);
            }
            // description in English has priority
            if (enDescription != null && !enDescription.isEmpty()) {
                r.setDescription(enDescription);
            }
        }
    }

    private static String cleanup(String x) {
        if (x == null) {
            return "";
        }
        x = x.replaceAll("&lt;br/&gt;", " ");
        x = x.replaceAll("<br/>", " ");
        x = x.replaceAll("[\t\n\r ]+", " ");
        return x.trim();
    }
}
