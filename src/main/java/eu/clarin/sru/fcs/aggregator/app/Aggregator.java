package eu.clarin.sru.fcs.aggregator.app;

import eu.clarin.sru.fcs.aggregator.search.Search;
import eu.clarin.sru.fcs.aggregator.cache.ScanCrawlTask;
import eu.clarin.sru.fcs.aggregator.cache.ScanCrawler;
import eu.clarin.sru.fcs.aggregator.cache.ScanCacheFile;
import eu.clarin.sru.fcs.aggregator.cache.SimpleInMemScanCache;
import eu.clarin.sru.client.SRUThreadedClient;
import eu.clarin.sru.client.SRUVersion;
import eu.clarin.sru.client.fcs.ClarinFCSRecordParser;
import eu.clarin.sru.fcs.aggregator.cache.EndpointUrlFilter;
import eu.clarin.sru.fcs.aggregator.registry.CenterRegistryLive;
import eu.clarin.sru.fcs.aggregator.cache.ScanCache;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import opennlp.tools.tokenize.TokenizerModel;

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
 */
public class Aggregator implements ServletContextListener {

	private static final Logger LOGGER = Logger.getLogger(Aggregator.class.getName());

	public static final int WAITING_TIME_FOR_SHUTDOWN_MS = 10000;
	public static final String DE_TOK_MODEL = "/tokenizer/de-tuebadz-8.0-token.bin";
	private static final String DEFAULT_DATA_LOCATION = "/data";
	private static final String SCAN_DIR_NAME = "scan";

	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private static Aggregator instance;

	private AtomicReference<ScanCache> scanCacheAtom = new AtomicReference<ScanCache>();
	private TokenizerModel model;
	private SRUThreadedClient sruClient = null;
	private Map<Long, Search> activeSearches = Collections.synchronizedMap(new HashMap<Long, Search>());

	public static Aggregator getInstance() {
		return instance;
	}

	public ScanCache getScanCache() {
		return scanCacheAtom.get();
	}

	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		LOGGER.info("Aggregator is starting now.");
		instance = this;
		try {
			sruClient = new SRUThreadedClient();
			sruClient.registerRecordParser(new ClarinFCSRecordParser());

			InitialContext context = new InitialContext();
			Integer cacheMaxDepth = (Integer) context.lookup("java:comp/env/scan-max-depth");
			EndpointUrlFilter filter //= null;
					= new EndpointUrlFilter("uni-tuebingen.de", ".mpi.nl", "dspin.dwds.de", "lindat.");
			ScanCrawler scanCrawler = new ScanCrawler(new CenterRegistryLive(), sruClient, filter, cacheMaxDepth);

			ScanCacheFile scanCacheFile = new ScanCacheFile(getScanDirectory());
			LOGGER.info("Start cache read");
			try {
				scanCacheAtom.set(scanCacheFile.read());
				LOGGER.info("Finished cache read, number of root corpora: " + scanCacheAtom.get().getRootCorpora().size());
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Error while reading the scan cache!", e);
				scanCacheAtom.set(new SimpleInMemScanCache());
			}

			String updateIntervalUnitString = (String) context.lookup("java:comp/env/update-interval-unit");
			TimeUnit cacheUpdateIntervalUnit = TimeUnit.valueOf(updateIntervalUnitString);
			Integer cacheUpdateInterval = (Integer) context.lookup("java:comp/env/update-interval");
			scheduler.scheduleAtFixedRate(
					new ScanCrawlTask(scanCrawler, scanCacheFile, scanCacheAtom),
					0, cacheUpdateInterval, cacheUpdateIntervalUnit);

			model = setUpTokenizers();
			LOGGER.info("Aggregator initialization finished.");
		} catch (Exception ex) {
			LOGGER.log(Level.SEVERE, null, ex);
			instance = null; // force crash
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		LOGGER.info("Aggregator is shutting down.");
		for (Search search : activeSearches.values()) {
			search.shutdown();
		}
		shutdownAndAwaitTermination(sruClient, scheduler);
		LOGGER.info("Aggregator shutdown complete.");
	}

	public static SRUVersion getSRUVersion(String sruversion) {
		SRUVersion version = SRUVersion.VERSION_1_2;
		if (sruversion.equals("1.2")) {
			version = SRUVersion.VERSION_1_2;
		} else if (sruversion.equals("1.1")) {
			version = SRUVersion.VERSION_1_1;
		} else {
			return null;
		}
		return version;
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

	private static String getScanDirectory() throws NamingException {
		InitialContext context = new InitialContext();
		String dataLocationPropertyName = (String) context.lookup("java:comp/env/data-location-property");
		String aggregatorDirName = (String) context.lookup("java:comp/env/aggregator-folder");
		// see if data location is set in properties
		String dataLocation = System.getProperty(dataLocationPropertyName);
		if (dataLocation == null || !(new File(dataLocation, aggregatorDirName).exists())) {
			dataLocation = DEFAULT_DATA_LOCATION;
			if (!(new File(dataLocation, aggregatorDirName).exists())) {
				dataLocation = System.getProperty("user.home");
			}
			if ((new File(dataLocation, aggregatorDirName).exists())) {
				LOGGER.info(dataLocationPropertyName + " property is not defined, "
						+ "setting to default: " + dataLocation);
			} else {
				LOGGER.info(dataLocationPropertyName + " property is not defined, "
						+ "default location does not exist: " + dataLocation);
				throw new RuntimeException("Data location not found");
			}
		}

		File aggregatorDir = new File(dataLocation, aggregatorDirName);
		if (!aggregatorDir.exists()) {
			LOGGER.severe("Aggregator directory does not exist: "
					+ aggregatorDir.getAbsolutePath());
		}
		File scanDir = new File(aggregatorDir, SCAN_DIR_NAME);
		if (!scanDir.exists()) {
			if (!scanDir.mkdir()) {
				LOGGER.severe("Scan directory does not exist and cannot be created: "
						+ aggregatorDir.getAbsolutePath());
			}
		}
		String scanPath = scanDir.getAbsolutePath();
		LOGGER.info("Scan data location: " + scanPath);
		return scanPath;
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
			InputStream tokenizerModelDeAsIS = Thread.currentThread().getContextClassLoader().getResourceAsStream(DE_TOK_MODEL);
			model = new TokenizerModel(tokenizerModelDeAsIS);
			tokenizerModelDeAsIS.close();
		} catch (IOException ex) {
			LOGGER.log(Level.SEVERE, "Failed to load tokenizer model", ex);
		}
		return model;
	}
}
