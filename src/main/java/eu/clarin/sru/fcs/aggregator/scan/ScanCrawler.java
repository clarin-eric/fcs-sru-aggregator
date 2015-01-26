package eu.clarin.sru.fcs.aggregator.scan;

import eu.clarin.sru.fcs.aggregator.util.CounterLatch;
import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUDiagnostic;
import eu.clarin.sru.client.SRUScanRequest;
import eu.clarin.sru.client.SRUScanResponse;
import eu.clarin.sru.client.SRUTerm;
import eu.clarin.sru.fcs.aggregator.client.ThrottledClient;
import eu.clarin.sru.fcs.aggregator.util.SRUCQL;
import eu.clarin.sru.fcs.aggregator.util.Throw;
import java.net.SocketTimeoutException;
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
			Iterable<String> endpoints = institution.getEndpoints();
			for (String endp : endpoints) {
				ScanTask st = new ScanTask(institution, endp, null, cache, 0);
				scanForCorpora(st);
			}
		}

		try {
			log.info("awaiting");
			latch.await();
		} catch (InterruptedException e) {
			log.error("INTERRUPTED wait for {} scan task(s), are we shutting down?", latch.get());
		}

		log.info("done crawling");
		return cache;
	}

	public Statistics getStatistics() {
		return statistics;
	}

	private void scanForCorpora(ScanTask st) {
		if (st.depth > maxDepth) {
			return;
		}

		SRUScanRequest scanRequest = null;
		try {
			scanRequest = new SRUScanRequest(st.endpointUrl);
			scanRequest.setScanClause(SRUCQL.SCAN_RESOURCE_PARAMETER
					+ "=" + normalizeHandle(st.parentCorpus));
			scanRequest.setExtraRequestData(SRUCQL.SCAN_RESOURCE_INFO_PARAMETER,
					SRUCQL.SCAN_RESOURCE_INFO_PARAMETER_DEFAULT_VALUE);
		} catch (Throwable ex) {
			log.error("Exception creating scan request for {}: {}", st.endpointUrl, ex.getMessage());
			log.error("--> ", ex);
		}
		if (scanRequest == null) {
			return;
		}

		log.info("{} Start scan: {}#{}", latch.get(), st.endpointUrl, normalizeHandle(st.parentCorpus));
		latch.increment();
		sruClient.scan(scanRequest, st);
	}

	class ScanTask implements ThrottledClient.ScanCallback {

		final Institution institution;
		final String endpointUrl;
		final Corpus parentCorpus;
		final Corpora corpora;
		final int depth;

		ScanTask(final Institution institution, final String endpointUrl,
				final Corpus parentCorpus, final Corpora corpora, final int depth) {
			this.institution = institution;
			this.endpointUrl = endpointUrl;
			this.parentCorpus = parentCorpus;
			this.corpora = corpora;
			this.depth = depth;
		}

		@Override
		public void onSuccess(SRUScanResponse response, ThrottledClient.Stats stats) {
			try {
				statistics.addEndpointDatapoint(institution, endpointUrl, stats.getQueueTime(), stats.getExecutionTime());
				if (response != null && response.hasTerms()) {
					for (SRUTerm term : response.getTerms()) {
						if (term == null) {
							log.warn("null term for scan at endpoint {}", endpointUrl);
						} else {
							Corpus c = createCorpus(institution, endpointUrl, term);
							if (corpora.addCorpus(c, parentCorpus)) {
								ScanTask st = new ScanTask(institution, endpointUrl, c, corpora, depth + 1);
								scanForCorpora(st);
							}
						}
					}
				} else if (parentCorpus == null) {
					Corpus c = createCorpus(institution, endpointUrl, null);
					if (corpora.addCorpus(c, parentCorpus)) {
						ScanTask st = new ScanTask(institution, endpointUrl, c, corpora, depth + 1);
						scanForCorpora(st);
					}
				}

				if (response != null && response.hasDiagnostics()) {
					for (SRUDiagnostic d : response.getDiagnostics()) {
						SRUScanRequest request = response.getRequest();

						String handle = SRUCQL.SCAN_RESOURCE_PARAMETER + "=" + normalizeHandle(parentCorpus);
						Diagnostic diag = new Diagnostic(request.getBaseURI().toString(), handle,
								d.getURI(), d.getMessage(), d.getDetails());
						statistics.addEndpointDiagnostic(institution, endpointUrl, diag);
						log.info("Diagnostic: {} {}: {} {} {}", diag.getReqEndpointUrl(), diag.getReqContext(),
								diag.getDgnUri(), diag.getDgnMessage(), diag.getDgnDiagnostic());
					}
				}

				log.info("{} Finished scan: {}#{}", latch.get(), endpointUrl, normalizeHandle(parentCorpus));
			} catch (Exception xc) {
				log.error("{} Exception in callback {}#{}", latch.get(), endpointUrl, normalizeHandle(parentCorpus));
				log.error("--> ", xc);
			} finally {
				latch.decrement();
			}
		}

		@Override
		public void onError(SRUScanRequest request, SRUClientException error, ThrottledClient.Stats stats) {
			try {
				log.error("{} Error while scanning {}#{}: {}", latch.get(), endpointUrl, normalizeHandle(parentCorpus), error.getMessage());
				statistics.addEndpointDatapoint(institution, endpointUrl, stats.getQueueTime(), stats.getExecutionTime());
				statistics.addErrorDatapoint(institution, endpointUrl, error);
				if (Throw.isCausedBy(error, SocketTimeoutException.class)) {
					return;
				}
				log.error("--> " + request.getBaseURI() + "?" + request.getScanClause() + " --> ", error);
			} finally {
				latch.decrement();
			}
		}
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

	private static Corpus createCorpus(Institution institution, String endpointUrl, SRUTerm term) {
		Corpus c = new Corpus(institution, endpointUrl);
		c.setDisplayName(term.getDisplayTerm());
		String handle = term.getValue();
		if (handle == null) {
			log.error("null handle for corpus: {} : {}", endpointUrl, term.getDisplayTerm());
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
