package eu.clarin.sru.fcs.aggregator.cache;

import eu.clarin.sru.fcs.aggregator.util.CounterLatch;
import eu.clarin.sru.client.SRUCallback;
import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUScanRequest;
import eu.clarin.sru.client.SRUScanResponse;
import eu.clarin.sru.client.SRUTerm;
import eu.clarin.sru.client.SRUThreadedClient;
import eu.clarin.sru.fcs.aggregator.registry.CenterRegistry;
import eu.clarin.sru.fcs.aggregator.registry.Corpus;
import eu.clarin.sru.fcs.aggregator.registry.Institution;
import eu.clarin.sru.fcs.aggregator.util.SRUCQL;
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
	private final SRUThreadedClient sruScanClient;
	private final int maxDepth;
	private final EndpointFilter filter;
	private final CounterLatch latch;

	public ScanCrawler(CenterRegistry centerRegistry, SRUThreadedClient sruScanClient, EndpointFilter filter, int maxDepth) {
		this.centerRegistry = centerRegistry;
		this.sruScanClient = sruScanClient;
		this.maxDepth = maxDepth;
		this.filter = filter;
		this.latch = new CounterLatch();
	}

	public Corpora crawl() {
		Corpora cache = new Corpora();
		for (Institution institution : centerRegistry.getCQLInstitutions()) {
			cache.addInstitution(institution);
			Iterable<String> endpoints = institution.getEndpoints();
			if (filter != null) {
				endpoints = filter.filter(endpoints);
			}
			for (String endp : endpoints) {
				addCorpora(institution, endp, null, cache, 0);
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

	private void addCorpora(final Institution institution, final String endpointUrl,
			final Corpus parentCorpus, final Corpora corpora, final int depth) {
		if (depth > maxDepth) {
			return;
		}

		SRUScanRequest scanRequest = null;
		try {
			scanRequest = new SRUScanRequest(endpointUrl);
			scanRequest.setScanClause(SRUCQL.SCAN_RESOURCE_PARAMETER
					+ "=" + normalizeHandle(parentCorpus));
			scanRequest.setExtraRequestData(SRUCQL.SCAN_RESOURCE_INFO_PARAMETER,
					SRUCQL.SCAN_RESOURCE_INFO_PARAMETER_DEFAULT_VALUE);
		} catch (Exception ex) {
			log.error("Exception creating scan request for {}: {}", endpointUrl, ex.getMessage());
		}
		if (scanRequest == null) {
			return;
		}

		log.info("{} Start scan: {}", latch.get(), endpointUrl);
		latch.increment();
		try {
			sruScanClient.scan(scanRequest, new SRUCallback<SRUScanRequest, SRUScanResponse>() {
				@Override
				public void onSuccess(SRUScanResponse response) {
					try {
						if (response != null && response.hasTerms()) {
							for (SRUTerm term : response.getTerms()) {
								Corpus c = createCorpus(institution, endpointUrl, term);
								if (corpora.addCorpus(c, parentCorpus)) {
									addCorpora(institution, endpointUrl, c, corpora, depth + 1);
								}
							}
						} else if (parentCorpus == null) {
							Corpus c = createCorpus(institution, endpointUrl, null);
							if (corpora.addCorpus(c, parentCorpus)) {
								addCorpora(institution, endpointUrl, c, corpora, depth + 1);
							}
						}

						log.info("{} Finished scan: {}", latch.get(), endpointUrl);
					} catch (Exception xc) {
						log.error("{} Exception in callback {}", latch.get(), endpointUrl);
						log.error("--> ", xc);
					} finally {
						latch.decrement();
					}
				}

				@Override

				public void onError(SRUScanRequest request, SRUClientException error) {
					latch.decrement();
					log.error("{} Error while scanning {}: {} : {}", latch.get(), endpointUrl, error, error.getCause());
				}
			}
			);
		} catch (SRUClientException ex) {
			latch.decrement();
			log.error("{} Exception in scan request for {}: {}", latch.get(), endpointUrl, ex.getMessage());
		}
	}

	private static String normalizeHandle(Corpus corpus) {
		if (corpus == null) {
			return Corpus.ROOT_HANDLE;
		}
		String handle = corpus.getHandle();
		if (Corpus.HANDLE_WITH_SPECIAL_CHARS.matcher(handle).matches()) {
			//resourceValue = "%22" + resourceValue + "%22";
			handle = "\"" + handle + "\"";
		}
		return handle;
	}

	private static Corpus createCorpus(Institution institution, String endpointUrl, SRUTerm term) {
		Corpus c = new Corpus(institution, endpointUrl);
		if (term == null) {
			c.setDisplayName("[" + endpointUrl + "]");
		} else {
			c.setDisplayName(term.getDisplayTerm());
			c.setHandle(term.getValue());
			if (term.getNumberOfRecords() > 0) {
				c.setNumberOfRecords(term.getNumberOfRecords());
			}
			addExtraInfo(c, term);
		}
		return c;
	}

	// TODO: ask Oliver to add API support for the extra info in the
	// SRU client/server libraries, so that it's not necessary to work
	// with DocumentFragment
	private static void addExtraInfo(Corpus c, SRUTerm term) {
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
}
