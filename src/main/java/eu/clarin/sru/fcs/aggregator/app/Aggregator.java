package eu.clarin.sru.fcs.aggregator.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import com.optimaize.langdetect.text.*;
import eu.clarin.sru.fcs.aggregator.search.Search;
import eu.clarin.sru.fcs.aggregator.scan.ScanCrawlTask;
import eu.clarin.sru.fcs.aggregator.scan.Corpora;
import eu.clarin.sru.client.SRUVersion;
import eu.clarin.sru.client.fcs.ClarinFCSClientBuilder;
import eu.clarin.sru.client.fcs.ClarinFCSEndpointDescriptionParser;
import eu.clarin.sru.fcs.aggregator.client.ThrottledClient;
import eu.clarin.sru.fcs.aggregator.scan.Corpus;
import eu.clarin.sru.fcs.aggregator.rest.RestService;
import eu.clarin.sru.fcs.aggregator.scan.Statistics;
import eu.clarin.sru.fcs.aggregator.util.LanguagesISO693;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.jetty.server.session.SessionHandler;
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
 * mechanism is currently used by VLO.
 *
 * Example: POST
 * http://weblicht.sfs.uni-tuebingen.de/Aggregator HTTP/1.1
 * query = bellen & x-aggregation-context =
 * {"http://fedora.clarin-d.uni-saarland.de/sru/":["hdl:11858/00-246C-0000-0008-5F2A-0"]}
 *
 * If the Aggregator web page has the URL query string parameter 'mode'
 * set to the string 'search', and the 'query' parameter is set,
 * then the aggregator search results for this query are immediately displayed
 * (i.e. users don't need to click 'search' in the aggregator page).
 * This feature has been requested initially by Martin Wynne from CLARIN ERIC
 *
 * @author Yana Panchenko
 * @author edima
 *
 * TODO: improve help page text
 *
 * TODO: update comments everywhere
 *
 * TODO: add some basic docs
 *
 * TODO: Export search results to personal workspace using oauth
 *
 * TODO: websockets
 *
 * TODO: show multiple hits on the same result in multiple rows, linked visually
 *
 * TODO: optimise page load
 *
 * TODO: test it in IE, other browsers
 *
 * TODO: tri-state for parent collections; search + message implications
 *
 */
public class Aggregator extends Application<AggregatorConfiguration> {

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(Aggregator.class);

	final int SEARCHES_SIZE_GC_THRESHOLD = 1000;
	final int SEARCHES_AGE_GC_THRESHOLD = 60;

	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private static Aggregator instance;
	private AggregatorConfiguration.Params params;

	private AtomicReference<Corpora> scanCacheAtom = new AtomicReference<Corpora>(new Corpora());
	private AtomicReference<Statistics> scanStatsAtom = new AtomicReference<Statistics>(new Statistics());
	private AtomicReference<Statistics> searchStatsAtom = new AtomicReference<Statistics>(new Statistics());

	private LanguageDetector languageDetector;
	private TextObjectFactory textObjectFactory;

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
		bootstrap.addBundle(new AssetsBundle("/assets", "/", "index.html", "static"));
	}

	@Override
	public void run(AggregatorConfiguration config, Environment environment) throws Exception {
		params = config.aggregatorParams;
		instance = this;

		List<String> wll = new ArrayList<String>();
		for (String l : config.aggregatorParams.weblichtConfig.getAcceptedTcfLanguages()) {
			wll.add(LanguagesISO693.getInstance().code_3ForCode(l));
		}
		config.aggregatorParams.weblichtConfig.acceptedTcfLanguages = wll;

		System.out.println("Using parameters: ");
		try {
			System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().
					writeValueAsString(config.aggregatorParams));
		} catch (IOException xc) {
		}

		environment.getApplicationContext().setSessionHandler(new SessionHandler());
		environment.getApplicationContext().setErrorHandler(new ErrorHandler());
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

	public void init() throws IOException {
		log.info("Aggregator initialization started.");
		sruScanClient = new ThrottledClient(
				new ClarinFCSClientBuilder()
				.setConnectTimeout(params.ENDPOINTS_SCAN_TIMEOUT_MS)
				.setSocketTimeout(params.ENDPOINTS_SCAN_TIMEOUT_MS)
				.addDefaultDataViewParsers()
				.registerExtraResponseDataParser(
						new ClarinFCSEndpointDescriptionParser())
				.enableLegacySupport()
				.buildThreadedClient(),
				params.SCAN_MAX_CONCURRENT_REQUESTS_PER_ENDPOINT
		);
		sruSearchClient = new ThrottledClient(
				new ClarinFCSClientBuilder()
				.setConnectTimeout(params.ENDPOINTS_SEARCH_TIMEOUT_MS)
				.setSocketTimeout(params.ENDPOINTS_SEARCH_TIMEOUT_MS)
				.addDefaultDataViewParsers()
				.enableLegacySupport()
				.buildThreadedClient(),
				params.SEARCH_MAX_CONCURRENT_REQUESTS_PER_ENDPOINT
		);

		File corporaCacheFile = new File(params.AGGREGATOR_FILE_PATH);
		File corporaOldCacheFile = new File(params.AGGREGATOR_FILE_PATH_BACKUP);

		// init corpora from file
		{
			Corpora corpora = null;
			try {
				corpora = new ObjectMapper().readValue(corporaCacheFile, Corpora.class);
			} catch (Exception xc) {
				log.error("Failed to load cached corpora from primary file:", xc);
			}
			if (corpora == null) {
				try {
					corpora = new ObjectMapper().readValue(corporaOldCacheFile, Corpora.class);
				} catch (Exception e) {
					log.error("Failed to load cached corpora from backup file:", e);
				}
			}
			if (corpora != null) {
				scanCacheAtom.set(corpora);
				log.info("corpus list read from file; number of root corpora: " + scanCacheAtom.get().getCorpora().size());
			}
		}

		LanguagesISO693.getInstance(); // force init
		initLanguageDetector();

		ScanCrawlTask task = new ScanCrawlTask(sruScanClient,
				params.CENTER_REGISTRY_URL, params.SCAN_MAX_DEPTH,
				params.additionalCQLEndpoints,
				null, scanCacheAtom, corporaCacheFile, corporaOldCacheFile,
				scanStatsAtom, searchStatsAtom);
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
	public Search startSearch(SRUVersion version, List<Corpus> corpora,
			String searchString, String searchLang,
			int firstRecord, int maxRecords) throws Exception {
		if (corpora.isEmpty()) {
			// No corpora
			return null;
		} else if (searchString.isEmpty()) {
			// No query
			return null;
		} else {
			Search sr = new Search(sruSearchClient, version, searchStatsAtom.get(),
					corpora, searchString, searchLang, maxRecords);
			if (activeSearches.size() > SEARCHES_SIZE_GC_THRESHOLD) {
				List<Long> toBeRemoved = new ArrayList<Long>();
				long t0 = System.currentTimeMillis();
				for (Map.Entry<Long, Search> e : activeSearches.entrySet()) {
					long dtmin = (t0 - e.getValue().getCreatedAt()) / 1000 / 60;
					if (dtmin > SEARCHES_AGE_GC_THRESHOLD) {
						log.info("removing search " + e.getKey() + ": " + dtmin + " minutes old");
						toBeRemoved.add(e.getKey());
					}
				}
				for (Long l : toBeRemoved) {
					activeSearches.remove(l);
				}
			}
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

	public void initLanguageDetector() throws IOException {
		List<LanguageProfile> languageProfiles = new LanguageProfileReader().readAll();
		languageDetector = LanguageDetectorBuilder
				.create(NgramExtractors.standard())
				.withProfiles(languageProfiles)
				.build();

		textObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText();
	}

	public String detectLanguage(String text) {
		return languageDetector.detect(textObjectFactory.forText(text)).orNull();
	}
}
