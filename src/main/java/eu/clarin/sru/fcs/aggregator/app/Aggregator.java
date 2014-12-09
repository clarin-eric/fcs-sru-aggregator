package eu.clarin.sru.fcs.aggregator.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.clarin.sru.fcs.aggregator.search.Search;
import eu.clarin.sru.fcs.aggregator.scan.ScanCrawlTask;
import eu.clarin.sru.fcs.aggregator.scan.Corpora;
import eu.clarin.sru.client.SRUVersion;
import eu.clarin.sru.client.fcs.ClarinFCSClientBuilder;
import eu.clarin.sru.fcs.aggregator.client.ThrottledClient;
import eu.clarin.sru.fcs.aggregator.scan.Corpus;
import eu.clarin.sru.fcs.aggregator.rest.RestService;
import eu.clarin.sru.fcs.aggregator.scan.Statistics;
import eu.clarin.sru.fcs.aggregator.lang.LanguagesISO693_3;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
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
import opennlp.tools.tokenize.TokenizerModel;
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
 * TODO: make language show nicely in the UI
 *
 * TODO: use selected visible corpus for search
 *
 * TODO: use language selection to hide corpora
 *
 * TODO: support new spec-compatible centres, see Oliver's mail ...............
 *
 * TODO: disable popups easily
 *
 * TODO: zoom into the results from a corpus, allow functionality only for the
 * view (search for next set of results)
 *
 * TODO: Fix activeSearch memory leak (gc searches older than...)
 *
 * TODO: Use weblicht with results
 *
 * TODO: Export to personal workspace as csv, excel, tcf, plain text
 *
 * TODO: Download to personal workspace as csv, excel, tcf, plain text
 *
 * TODO: use SRUClient's extraResponseData POJOs
 *
 * TODO: websockets
 *
 * TODO: atomic replace of cached corpora (file)
 *
 * TODO: show multiple hits on the same result in multiple rows, linked visually
 *
 */
public class Aggregator extends Application<AggregatorConfiguration> {

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(Aggregator.class);

	public static final String DE_TOK_MODEL = "tokenizer/de-tuebadz-8.0-token.bin";

	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private static Aggregator instance;
	private AggregatorConfiguration.Params params;

	private AtomicReference<Corpora> scanCacheAtom = new AtomicReference<Corpora>(new Corpora());
	private AtomicReference<Statistics> scanStatsAtom = new AtomicReference<Statistics>(new Statistics());
	private AtomicReference<Statistics> searchStatsAtom = new AtomicReference<Statistics>(new Statistics());

	private TokenizerModel model;
	private ThrottledClient sruScanClient = null;
	private ThrottledClient sruSearchClient = null;
	private Map<Long, Search> activeSearches = Collections.synchronizedMap(new HashMap<Long, Search>());

	public static void main(String[] args) throws Exception {
		new Aggregator().run(args);
	}

	@Override
	public String getName() {
		return "FCS Aggregator";
	}

	@Override
	public void initialize(Bootstrap<AggregatorConfiguration> bootstrap) {
		bootstrap.addBundle(new AssetsBundle("/assets", "/", "index.html"));
	}

	@Override
	public void run(AggregatorConfiguration config, Environment environment) {
		params = config.aggregatorParams;
		instance = this;

		System.out.println("Using parameters: ");
		try {
			System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().
					writeValueAsString(config.aggregatorParams));
		} catch (IOException xc) {
		}

		environment.jersey().setUrlPattern("/rest/*");
		environment.jersey().register(new RestService());

		try {
			init();
		} catch (Exception ex) {
			log.error("INIT EXCEPTION", ex);
			throw ex; // force exit
		}
	}

	public static Aggregator getInstance() {
		return instance;
	}

	public AggregatorConfiguration.Params getParams() {
		return params;
	}

	public Corpora getCorpora() {
		return scanCacheAtom.get();
	}

	public Statistics getScanStatistics() {
		return scanStatsAtom.get();
	}

	public Statistics getSearchStatistics() {
		return searchStatsAtom.get();
	}

	public void init() {
		log.info("Aggregator initialization started.");
		sruScanClient = new ThrottledClient(
				new ClarinFCSClientBuilder()
				.setConnectTimeout(params.ENDPOINTS_SCAN_TIMEOUT_MS)
				.setSocketTimeout(params.ENDPOINTS_SCAN_TIMEOUT_MS)
				.addDefaultDataViewParsers()
				.enableLegacySupport()
				.buildThreadedClient());
		sruSearchClient = new ThrottledClient(
				new ClarinFCSClientBuilder()
				.setConnectTimeout(params.ENDPOINTS_SEARCH_TIMEOUT_MS)
				.setSocketTimeout(params.ENDPOINTS_SEARCH_TIMEOUT_MS)
				.addDefaultDataViewParsers()
				.enableLegacySupport()
				.buildThreadedClient());

		File corporaCacheFile = new File(params.AGGREGATOR_FILE_PATH);
		try {
			Corpora corpora = new ObjectMapper().readValue(corporaCacheFile, Corpora.class);
			scanCacheAtom.set(corpora);
			log.info("corpus list read from file; number of root corpora: " + scanCacheAtom.get().getCorpora().size());
		} catch (Exception e) {
			log.error("Error while reading cached corpora:", e);
		}

		LanguagesISO693_3.getInstance(); // force init
		model = setUpTokenizers();

		ScanCrawlTask task = new ScanCrawlTask(sruScanClient,
				params.CENTER_REGISTRY_URL, params.SCAN_MAX_DEPTH,
				null, scanCacheAtom, corporaCacheFile, scanStatsAtom);
		scheduler.scheduleAtFixedRate(task, params.SCAN_TASK_INITIAL_DELAY,
				params.SCAN_TASK_INTERVAL, params.getScanTaskTimeUnit());

		log.info("Aggregator initialization finished.");
	}

	public void shutdown(AggregatorConfiguration config) {
		log.info("Aggregator is shutting down.");
		for (Search search : activeSearches.values()) {
			search.shutdown();
		}
		shutdownAndAwaitTermination(config.aggregatorParams, sruScanClient, scheduler);
		shutdownAndAwaitTermination(config.aggregatorParams, sruSearchClient, scheduler);
		log.info("Aggregator shutdown complete.");
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
			Search sr = new Search(sruSearchClient, version, searchStatsAtom.get(),
					corpora, searchString, searchLang, 1, maxRecords);
			activeSearches.put(sr.getId(), sr);
			return sr;
		}
	}

	public Search getSearchById(Long id) {
		return activeSearches.get(id);
	}

	private static void shutdownAndAwaitTermination(AggregatorConfiguration.Params params,
			ThrottledClient sruClient, ExecutorService scheduler) {
		try {
			sruClient.shutdown();
			scheduler.shutdown();
			Thread.sleep(params.EXECUTOR_SHUTDOWN_TIMEOUT_MS);
			sruClient.shutdownNow();
			scheduler.shutdownNow();
			Thread.sleep(params.EXECUTOR_SHUTDOWN_TIMEOUT_MS);
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

//		filter = new EndpointUrlFilterAllow("lindat");
//		filter = new EndpointUrlFilterDeny("leipzig");
//		filter = new EndpointUrlFilterAllow("leipzig", "mpi.nl");
//		filter = new EndpointUrlFilterAllow("lindat");
}
