package eu.clarin.sru.fcs.aggregator.app;

import eu.clarin.sru.fcs.aggregator.search.Search;
import eu.clarin.sru.fcs.aggregator.cache.ScanCrawlTask;
import eu.clarin.sru.fcs.aggregator.cache.Corpora;
import eu.clarin.sru.client.SRUThreadedClient;
import eu.clarin.sru.client.SRUVersion;
import eu.clarin.sru.client.fcs.ClarinFCSClientBuilder;
import eu.clarin.sru.fcs.aggregator.cache.EndpointUrlFilter;
import eu.clarin.sru.fcs.aggregator.registry.Corpus;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import opennlp.tools.tokenize.TokenizerModel;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.LoggerFactory;

/**
 * Main component of the Aggregator application intended to provide users access
 * to CLARIN-FCS resources.
 *
 * The webapp base URL corresponds to the default behavior of displaying the
 * main aggregator page, where the user can enter query, select the resources of
 * CQL endpoints (as specified in the Clarin center registry), and search in
 * these resources. The endpoints/resources selection is optional, by default
 * all the endpoints root resources are selected.
 *
 * If invoked with 'x-aggregation-context' and 'query' parameter, the aggregator
 * will pre-select provided resources and fill in the query field. This
 * mechanism is currently used by VLO. Example: POST
 * http://weblicht.sfs.uni-tuebingen.de/Aggregator HTTP/1.1 operation =
 * searchRetrieve & version = 1.2 & query = bellen & x-aggregation-context =
 * {"http://fedora.clarin-d.uni-saarland.de/sru/":["hdl:11858/00-246C-0000-0008-5F2A-0"]}
 *
 *
 * Additionally, if run with the a URL query string parameter 'mode', the
 * special behavior of the aggregator is triggered:
 *
 * /?mode=testing corresponds to the mode where the CQL endpoints are taken not
 * from Clarin center repository, but from a hard-coded endpoints list; this
 * functionality is useful for testing the development instances of endpoints,
 * before they are moved to production. Was done to meet the request from MPI.
 *
 * /?mode=search corresponds to the mode where the aggregator page is requested
 * with the already known query and (optionally) resources to search in, and if
 * the immediate search is desired. In this case the aggregator search results
 * page is displayed and search results of the provided query start to fill it
 * in immediately (i.e. users don't need to click 'search' in the aggregator
 * page). Was done to meet the request from CLARIN ERIC (Martin Wynne contacted
 * us).
 *
 * /?mode=live corresponds to the mode where the information about corpora are
 * taken not from the scan cache (crawled in advance), but loaded live, starting
 * from the request to center registry and then performing scan operation
 * requests on each CQL endpoint listed there. It takes time to get the
 * corresponding responses from the endpoints, therefore the Aggregator page
 * loads very slow in this mode. But this mode is useful for testing of the
 * newly added or changed corpora without waiting for the next crawl.
 *
 *
 * Adds Application initialization and clean up: only one SRU threaded client is
 * used in the application, it has to be shut down when the application stops.
 * One Languages object instance is used within the application.
 *
 * @author Yana Panchenko
 * @author edima
 *
 * TODO: result panes with animation and more info
 *
 * TODO: highlighted/kwic hits: toggle for now
 *
 * TODO: show multiple hits on the same result in multiple rows, linked visually
 *
 * TODO: new UI element to specify layer we search in
 *
 * TODO: good UI for tree view corpus selection, with instant search form
 *
 * TODO: zoom into the results from a corpus, allow functionality only for the
 * view
 *
 * TODO: websockets, selfhosting
 *
 * TODO: add statistics menu option w/ page
 *
 * TODO: atomic replace of cached corpora (file)
 *
 * TODO: test json deserialization
 *
 */
public class Aggregator implements ServletContextListener {

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(Aggregator.class);

	public static final int WAITING_TIME_FOR_SHUTDOWN_MS = 2000;
	public static final String DE_TOK_MODEL = "/tokenizer/de-tuebadz-8.0-token.bin";
	private static final String DEFAULT_DATA_LOCATION = "/data";

	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private static Aggregator instance;

	private AtomicReference<Corpora> scanCacheAtom = new AtomicReference<Corpora>(new Corpora());
	private TokenizerModel model;
	private SRUThreadedClient sruClient = null;
	private Map<Long, Search> activeSearches = Collections.synchronizedMap(new HashMap<Long, Search>());
	private Params params;

	public static Aggregator getInstance() {
		return instance;
	}

	public Corpora getCorpora() {
		return scanCacheAtom.get();
	}

	public Params getParams() {
		return params;
	}

	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		log.info("Aggregator is starting now.");
		instance = this;
		try {
			params = new Params();

			sruClient = new ClarinFCSClientBuilder()
					.setConnectTimeout(5000)
					.setSocketTimeout(5000)
					.addDefaultDataViewParsers()
					.enableLegacySupport()
					.buildThreadedClient();

			File corporaCacheFile = new File(params.aggregatorFilePath);
			try {
				Corpora corpora = new ObjectMapper().readValue(corporaCacheFile, Corpora.class);
				scanCacheAtom.set(corpora);
				log.info("corpus list read from file; number of root corpora: " + scanCacheAtom.get().getCorpora().size());
			} catch (Exception e) {
				log.error("Error while reading cached corpora:", e);
			}

			model = setUpTokenizers();

			EndpointUrlFilter filter = new EndpointUrlFilter();//.deny("leipzig"); // ~5k corpora
			ScanCrawlTask task = new ScanCrawlTask(sruClient, params.centerRegistryUrl,
					params.cacheMaxDepth, filter, scanCacheAtom, corporaCacheFile);
			scheduler.scheduleAtFixedRate(task, 0, params.cacheUpdateInterval, params.cacheUpdateIntervalUnit);

			log.info("Aggregator initialization finished.");
		} catch (Exception ex) {
			log.error("INIT EXCEPTION", ex);
			instance = null; // force crash
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		log.info("Aggregator is shutting down.");
		for (Search search : activeSearches.values()) {
			search.shutdown();
		}
		shutdownAndAwaitTermination(sruClient, scheduler);
		log.info("Aggregator shutdown complete.");
	}

	public static SRUVersion getSRUVersion(String sruversion) {
		if (sruversion.equals("1.2")) {
			return SRUVersion.VERSION_1_2;
		} else if (sruversion.equals("1.1")) {
			return SRUVersion.VERSION_1_1;
		}
		return null;
	}

	// this function should be thread-safe
	public Search startSearch(SRUVersion version, List<Corpus> corpora, String searchString, String searchLang, int maxRecords) throws Exception {
		if (corpora.isEmpty()) {
			// No corpora
			return null;
		} else if (searchString.isEmpty()) {
			// No query
			return null;
		} else {
			Search sr = new Search(sruClient, version, corpora, searchString, searchLang, 1, maxRecords);
			activeSearches.put(sr.getId(), sr);
			return sr;
		}
	}

	public Search getSearchById(Long id) {
		return activeSearches.get(id);
	}

	private static void shutdownAndAwaitTermination(SRUThreadedClient sruClient, ExecutorService scheduler) {
		try {
			sruClient.shutdown();
			scheduler.shutdown();
			Thread.sleep(WAITING_TIME_FOR_SHUTDOWN_MS);
			sruClient.shutdownNow();
			scheduler.shutdownNow();
			Thread.sleep(WAITING_TIME_FOR_SHUTDOWN_MS);
		} catch (InterruptedException ie) {
			sruClient.shutdownNow();
			scheduler.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	private static TokenizerModel setUpTokenizers() {
		TokenizerModel model = null;
		try {
			try (InputStream tokenizerModelDeAsIS = Thread.currentThread().getContextClassLoader().getResourceAsStream(DE_TOK_MODEL)) {
				model = new TokenizerModel(tokenizerModelDeAsIS);
			}
		} catch (IOException ex) {
			log.error("Failed to load tokenizer model", ex);
		}
		return model;
	}
}
