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
 */
public class ScanCrawler {

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(ScanCrawler.class);

	private final CenterRegistry centerRegistry;
	private final int maxDepth;
	private final CounterLatch latch;
	private final ThrottledClient sruClient;
	private final Statistics statistics = new Statistics();

	public ScanCrawler(CenterRegistry centerRegistry, ThrottledClient sruClient, int maxDepth) {
		this.centerRegistry = centerRegistry;
		this.sruClient = sruClient;
		this.maxDepth = maxDepth;
		this.latch = new CounterLatch();
	}

	public Corpora crawl() {
		Corpora cache = new Corpora();
		for (Institution institution : centerRegistry.getCQLInstitutions()) {
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

		for (Institution institution : centerRegistry.getCQLInstitutions()) {
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
		final Corpora corpora;

		ExplainTask(final Institution institution, final Endpoint endpoint, final Corpora corpora) {
			this.institution = institution;
			this.endpoint = endpoint;
			this.corpora = corpora;
		}

		void start() {
			SRUExplainRequest explainRequest = null;
			try {
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
				statistics.addEndpointDatapoint(institution, endpoint.getUrl(), stats.getQueueTime(), stats.getExecutionTime());
				if (response != null && response.hasExtraResponseData()) {
					for (SRUExtraResponseData data : response.getExtraResponseData()) {
						if (data instanceof ClarinFCSEndpointDescription) {
							endpoint.setProtocol(FCSProtocolVersion.VERSION_1);
							ClarinFCSEndpointDescription desc = (ClarinFCSEndpointDescription) data;
							addCorpora(corpora, institution, endpoint, desc.getResources(), null);
						}
					}
				}

				if (response != null && response.hasDiagnostics()) {
					for (SRUDiagnostic d : response.getDiagnostics()) {
						SRUExplainRequest request = response.getRequest();
						Diagnostic diag = new Diagnostic(request.getBaseURI().toString(), null,
								d.getURI(), d.getMessage(), d.getDetails());
						statistics.addEndpointDiagnostic(institution, endpoint.getUrl(), diag);
						log.info("Diagnostic: {} {}: {} {} {}", diag.getReqEndpointUrl(), diag.getReqContext(),
								diag.getDgnUri(), diag.getDgnMessage(), diag.getDgnDiagnostic());
					}
				}

				log.info("{} Finished explain, endpoint version is {}: {}", latch.get(), endpoint.getProtocol(), endpoint.getUrl());
			} catch (Exception xc) {
				log.error("{} Exception in explain callback {}", latch.get(), endpoint.getUrl());
				log.error("--> ", xc);
			} finally {
				if (endpoint.getProtocol().equals(FCSProtocolVersion.LEGACY)) {
					new ScanTask(institution, endpoint, null, corpora, 0).start();
				}

				latch.decrement();
			}
		}

		@Override
		public void onError(SRUExplainRequest request, SRUClientException error, ThrottledClient.Stats stats) {
			try {
				log.error("{} Error while explaining {}: {}", latch.get(), endpoint.getUrl(), error.getMessage());
				statistics.addEndpointDatapoint(institution, endpoint.getUrl(), stats.getQueueTime(), stats.getExecutionTime());
				statistics.addErrorDatapoint(institution, endpoint.getUrl(), error);
				if (Throw.isCausedBy(error, SocketTimeoutException.class)) {
					return;
				}
				log.error("--> " + request.getBaseURI() + " --> ", error);
			} finally {
				latch.decrement();
			}
		}
	}

	private static void addCorpora(Corpora corpora, Institution institution, Endpoint endpoint,
			List<ResourceInfo> resources, Corpus parentCorpus) {
		if (resources == null) {
			return;
		}
		for (ResourceInfo ri : resources) {
			Corpus c = new Corpus(institution, endpoint);
			c.setHandle(ri.getPid());
			c.setTitle(getBestValueFrom(ri.getTitle()));
			c.setDescription(getBestValueFrom(ri.getTitle()));
			c.setLanguages(new HashSet<String>(ri.getLanguages()));
			c.setLandingPage(ri.getLandingPageURI());

			if (corpora.addCorpus(c, parentCorpus)) {
				addCorpora(corpora, institution, endpoint, ri.getSubResources(), c);
			}
		}
	}

	private static String getBestValueFrom(Map<String, String> map) {
		String ret = map.get("en");
		if (ret == null || ret.trim().isEmpty()) {
			ret = map.get(null);
		}
		if (ret == null || ret.trim().isEmpty()) {
			ret = map.get("de");
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
		final Corpus parentCorpus;
		final Corpora corpora;
		final int depth;

		ScanTask(final Institution institution, final Endpoint endpoint,
				final Corpus parentCorpus, final Corpora corpora, final int depth) {
			this.institution = institution;
			this.endpoint = endpoint;
			this.parentCorpus = parentCorpus;
			this.corpora = corpora;
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
						+ "=" + normalizeHandle(parentCorpus));
				scanRequest.setExtraRequestData(SRUCQL.SCAN_RESOURCE_INFO_PARAMETER,
						SRUCQL.SCAN_RESOURCE_INFO_PARAMETER_DEFAULT_VALUE);
			} catch (Throwable ex) {
				log.error("Exception creating scan request for {}: {}", endpoint.getUrl(), ex.getMessage());
				log.error("--> ", ex);
			}
			if (scanRequest == null) {
				return;
			}

			log.info("{} Start scan: {}#{}", latch.get(), endpoint.getUrl(), normalizeHandle(parentCorpus));
			latch.increment();
			sruClient.scan(scanRequest, this);
		}

		@Override
		public void onSuccess(SRUScanResponse response, ThrottledClient.Stats stats) {
			try {
				statistics.addEndpointDatapoint(institution, endpoint.getUrl(), stats.getQueueTime(), stats.getExecutionTime());
				if (response != null && response.hasTerms()) {
					for (SRUTerm term : response.getTerms()) {
						if (term == null) {
							log.warn("null term for scan at endpoint {}", endpoint.getUrl());
						} else {
							Corpus c = createCorpus(institution, endpoint, term);
							if (corpora.addCorpus(c, parentCorpus)) {
								new ScanTask(institution, endpoint, c, corpora, depth + 1).start();
							}
						}
					}
				} else if (parentCorpus == null) {
					Corpus c = createCorpus(institution, endpoint, null);
					if (corpora.addCorpus(c, parentCorpus)) {
						new ScanTask(institution, endpoint, c, corpora, depth + 1).start();
					}
				}

				if (response != null && response.hasDiagnostics()) {
					for (SRUDiagnostic d : response.getDiagnostics()) {
						SRUScanRequest request = response.getRequest();

						String handle = SRUCQL.SCAN_RESOURCE_PARAMETER + "=" + normalizeHandle(parentCorpus);
						Diagnostic diag = new Diagnostic(request.getBaseURI().toString(), handle,
								d.getURI(), d.getMessage(), d.getDetails());
						statistics.addEndpointDiagnostic(institution, endpoint.getUrl(), diag);
						log.info("Diagnostic: {} {}: {} {} {}", diag.getReqEndpointUrl(), diag.getReqContext(),
								diag.getDgnUri(), diag.getDgnMessage(), diag.getDgnDiagnostic());
					}
				}

				log.info("{} Finished scan: {}#{}", latch.get(), endpoint.getUrl(), normalizeHandle(parentCorpus));
			} catch (Exception xc) {
				log.error("{} Exception in scan callback {}#{}", latch.get(), endpoint.getUrl(), normalizeHandle(parentCorpus));
				log.error("--> ", xc);
			} finally {
				latch.decrement();
			}
		}

		@Override
		public void onError(SRUScanRequest request, SRUClientException error, ThrottledClient.Stats stats) {
			try {
				log.error("{} Error while scanning {}#{}: {}", latch.get(), endpoint.getUrl(), normalizeHandle(parentCorpus), error.getMessage());
				statistics.addEndpointDatapoint(institution, endpoint.getUrl(), stats.getQueueTime(), stats.getExecutionTime());
				statistics.addErrorDatapoint(institution, endpoint.getUrl(), error);
				if (Throw.isCausedBy(error, SocketTimeoutException.class)) {
					return;
				}
				log.error("--> " + request.getBaseURI() + "?" + request.getScanClause() + " --> ", error);
			} finally {
				latch.decrement();
			}
		}
	}

	public Statistics getStatistics() {
		return statistics;
	}

	private static String normalizeHandle(Corpus corpus) {
		if (corpus == null) {
			return Corpus.ROOT_HANDLE;
		}
		String handle = corpus.getHandle();
		if (Corpus.HANDLE_WITH_SPECIAL_CHARS.matcher(handle).matches()) {
			handle = "\"" + handle + "\"";
		}
		return handle;
	}

	private static Corpus createCorpus(Institution institution, Endpoint endpoint, SRUTerm term) {
		Corpus c = new Corpus(institution, endpoint);
		c.setTitle(term.getDisplayTerm());
		String handle = term.getValue();
		if (handle == null) {
			log.error("null handle for corpus: {} : {}", endpoint, term.getDisplayTerm());
			handle = "";
		}
		c.setHandle(handle);
		if (term.getNumberOfRecords() > 0) {
			c.setNumberOfRecords(term.getNumberOfRecords());
		}
		addExtraInfo(c, term);
		return c;
	}

	private static void addExtraInfo(Corpus c, SRUTerm term) {
		DocumentFragment extraInfo = term.getExtraTermData();
		String enDescription = null, enTitle = null;
		if (extraInfo != null) {
			NodeList infoNodes = extraInfo.getChildNodes().item(0).getChildNodes();
			for (int i = 0; i < infoNodes.getLength(); i++) {
				Node infoNode = infoNodes.item(i);
				if (infoNode.getNodeType() == Node.ELEMENT_NODE && infoNode.getLocalName().equals("LandingPageURI")) {
					c.setLandingPage(infoNode.getTextContent().trim());
				} else if (infoNode.getNodeType() == Node.ELEMENT_NODE && infoNode.getLocalName().equals("Languages")) {
					NodeList languageNodes = infoNode.getChildNodes();
					for (int j = 0; j < languageNodes.getLength(); j++) {
						if (languageNodes.item(j).getNodeType() == Node.ELEMENT_NODE
								&& languageNodes.item(j).getLocalName().equals("Language")) {
							Element languageNode = (Element) languageNodes.item(j);
							String languageText = languageNode.getTextContent();
							if (languageText != null && !languageText.trim().isEmpty()) {
								c.addLanguage(languageText.trim());
							}
						}
					}
				} else if (infoNode.getNodeType() == Node.ELEMENT_NODE && infoNode.getLocalName().equals("Title")) {
					Element element = (Element) infoNode;
					String descr = infoNode.getTextContent().replaceAll("&lt;br/&gt;", " ");
					descr = descr.replaceAll("<br/>", " ");
					descr = descr.replaceAll("[\t\n\r ]+", " ");
					c.setTitle(descr.trim());
					//String lang = element.getAttributeNS("http://clarin.eu/fcs/1.0/resource-info", "lang");
					//System.out.println("ATTRIBUTE LANG: " + lang);
					if ("en".equals(element.getAttribute("xml:lang"))) {
						enTitle = c.getDescription();
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
			// title in Engish has priority
			if (enTitle != null && !enTitle.isEmpty()) {
				c.setTitle(enTitle);
			}
			// description in Engish has priority
			if (enDescription != null && !enDescription.isEmpty()) {
				c.setDescription(enDescription);
			}
		}
	}
}
