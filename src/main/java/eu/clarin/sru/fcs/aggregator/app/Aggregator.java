package eu.clarin.sru.fcs.aggregator.app;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import com.optimaize.langdetect.text.*;
import eu.clarin.sru.client.SRUThreadedClient;
import eu.clarin.sru.fcs.aggregator.search.Search;
import eu.clarin.sru.fcs.aggregator.scan.ScanCrawlTask;
import eu.clarin.sru.fcs.aggregator.scan.CenterRegistryLive;
import eu.clarin.sru.fcs.aggregator.scan.ClientFactory;
import eu.clarin.sru.fcs.aggregator.scan.Resources;
import eu.clarin.sru.client.SRUVersion;
import eu.clarin.sru.client.fcs.ClarinFCSClientBuilder;
import eu.clarin.sru.client.fcs.ClarinFCSEndpointDescription;
import eu.clarin.sru.client.fcs.ClarinFCSEndpointDescriptionParser;
import eu.clarin.sru.fcs.aggregator.client.MaxConcurrentRequestsCallback;
import eu.clarin.sru.fcs.aggregator.client.ThrottledClient;
import eu.clarin.sru.fcs.aggregator.scan.Resource;
import eu.clarin.sru.fcs.aggregator.rest.RestService;
import eu.clarin.sru.fcs.aggregator.scan.Statistics;
import eu.clarin.sru.fcs.aggregator.util.LanguagesISO693;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import io.swagger.v3.jaxrs2.SwaggerSerializers;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;

import org.eclipse.jetty.server.session.SessionHandler;
import org.slf4j.LoggerFactory;

/**  ---- FCS AGGREGATOR OVERVIEW ----
 *
 * The Aggregator is intended to provide users access to CLARIN-FCS resources.
 * The app has two components, the server (this Java code) and the client (HTML, JS)
 *
 * The server is built on Java with Dropwizard and has the following functions:
 * - it gets requests from the user-agent (browser) for html, css, js files
 *   and serves them. This is done automatically by Dropwizard.
 * - it gets REST requests from the client (the JS code in browser)
 *   and serves them. See the RestService class
 * - using an executor service, it scans periodically for FCS endpoints and
 *   gathers informations about the resources stored at each one. See the scan
 *   package and ScanCrawl class
 * - when prompted by the user, through a REST call, it searches the resources
 *   by sending requests to all the appropriate endpoints. See the search package
 *   and especially the Search class.
 *   Note: because sending too many concurrent requests to the FCS endpoints is
 *         considered poor manners, the communication with the endpoints
 *         is done by the ThrottledClient class, which queues the requests
 *         if necessary.
 *
 * The client is a javascript React + bootstrap application running in the browser.
 * It displays the user interface and communicates with the server passing the
 * appropriate requests from the user as REST calls. The data format is JSON.
 *
 * The base URL corresponds to the default behavior of displaying the
 * main aggregator page, where the user can enter query, select the resources of
 * CQL endpoints (as specified in the Clarin center registry), and search in
 * these resources. The endpoints/resources selection is optional, by default
 * all the endpoints root resources are selected.
 *
 * If invoked by a POST request with 'x-aggregation-context' and
 * 'query' parameters, the aggregator will pre-select provided resources and
 * fill in the query field. This mechanism is currently used by VLO.
 *
 * Example: POST
 * http://weblicht.sfs.uni-tuebingen.de/Aggregator HTTP/1.1
 * query = bellen & x-aggregation-context =
 * {"http://fedora.clarin-d.uni-saarland.de/sru/":["hdl:11858/00-246C-0000-0008-5F2A-0"]}
 *
 * If the URL query string parameter 'mode' is set to the string 'search',
 * and if the 'query' parameter is set, then the aggregator search results for
 * this query are immediately displayed (i.e. users don't need to click
 * 'search' in the aggregator page). This feature has been requested initially
 * by Martin Wynne from CLARIN ERIC
 *
 * ---- Things Still To Be Done or To Be Considered ----
 *
 *     - Phonetic search
 *     - Export search results to personal workspace using oauth
 *     - Websockets instead of clients polling the server
 *     - When having multiple hits in the same result,
 *       show the hit in multiple rows, linked visually
 *     - Optimise page load
 *     - Expand parents of x-aggregator-context selected resource
 */

/**
 * This is the main class of the Aggregator, caring about initialization
 * and global access to common data.
 *
 * @author Yana Panchenko
 * @author edima
 * @author ljo
 */
public class Aggregator extends Application<AggregatorConfiguration> {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Aggregator.class);

    public final static String NAME = "CLARIN FCS Aggregator";

    final int SEARCHES_SIZE_GC_THRESHOLD = 1000;
    final int SEARCHES_AGE_GC_THRESHOLD = 60;

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static Aggregator instance;
    private AggregatorConfiguration.Params params;

    private AtomicReference<Resources> scanCacheAtom = new AtomicReference<Resources>(new Resources());
    private AtomicReference<Statistics> scanStatsAtom = new AtomicReference<Statistics>(new Statistics());
    private AtomicReference<Statistics> searchStatsAtom = new AtomicReference<Statistics>(new Statistics());

    private LanguageDetector languageDetector;
    private TextObjectFactory textObjectFactory;

    private ThrottledClient sruClient = null;
    public MaxConcurrentRequestsCallback maxScanConcurrentRequestsCallback;
    public MaxConcurrentRequestsCallback maxSearchConcurrentRequestsCallback;
    private Map<Long, Search> activeSearches = Collections.synchronizedMap(new HashMap<Long, Search>());

    public static void main(String[] args) throws Exception {
        new Aggregator().run(args);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void initialize(Bootstrap<AggregatorConfiguration> bootstrap) {
        // Enable variable substitution with environment variables
        bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
                bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));

        // Static assets (available at root but need to be prefixed for jersey
        // resources otherwise path conflict at "/*")
        bootstrap.addBundle(new AssetsBundle("/assets/js", "/js", null, "static-js"));
        bootstrap.addBundle(new AssetsBundle("/assets/css", "/css", null, "static-css"));
        bootstrap.addBundle(new AssetsBundle("/assets/fonts", "/fonts", null, "static-fonts"));
        bootstrap.addBundle(new AssetsBundle("/assets/img", "/img", null, "static-img"));
        bootstrap.addBundle(new AssetsBundle("/assets/lib", "/lib", null, "static-lib"));
        bootstrap.addBundle(
                new AssetsBundle("/assets/clarinservices", "/clarinservices", null, "static-clarinservices"));

        // Template index.html with environment variables
        bootstrap.addBundle(new ViewBundle<AggregatorConfiguration>() {
            @Override
            public Map<String, Map<String, String>> getViewConfiguration(AggregatorConfiguration config) {
                return new HashMap<String, Map<String, String>>() {
                    {
                        put("mustache", new HashMap<String, String>());
                    }
                };
            }
        });
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
            System.out.println(
                    new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(config.aggregatorParams));
        } catch (IOException xc) {
        }

        environment.getApplicationContext().setSessionHandler(new SessionHandler());

        environment.jersey().setUrlPattern("/*");
        environment.jersey().register(new IndexResource());
        environment.jersey().register(new RestService());

        // pretty printing
        if (config.aggregatorParams.prettyPrintJSON) {
            environment.getObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true);
        }

        // swagger
        if (config.aggregatorParams.openapiEnabled) {
            final String[] resourceClasses = { "eu.clarin.sru.fcs.aggregator.app.IndexResource",
                    "eu.clarin.sru.fcs.aggregator.rest.RestService" };
            final SwaggerConfiguration oasConfiguration = new SwaggerConfiguration()
                    .openAPI(new OpenAPI().addServersItem(
                            new Server().url(config.aggregatorParams.SERVER_URL).description("Local API endpoint")))
                    .prettyPrint(true)
                    .readAllResources(true)
                    .resourceClasses(Arrays.stream(resourceClasses).collect(Collectors.toSet()));
            environment.jersey().register(new OpenApiResource().openApiConfiguration(oasConfiguration));
            environment.jersey().register(new SwaggerSerializers());
        }

        try {
            init(environment);
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

    public Resources getResources() {
        return scanCacheAtom.get();
    }

    public Statistics getScanStatistics() {
        return scanStatsAtom.get();
    }

    public Statistics getSearchStatistics() {
        return searchStatsAtom.get();
    }

    public void init(Environment environment) throws IOException {
        log.info("Aggregator initialization started.");

        SRUThreadedClient sruScanClient = new ClarinFCSClientBuilder()
                .setConnectTimeout(params.ENDPOINTS_SCAN_TIMEOUT_MS)
                .setSocketTimeout(params.ENDPOINTS_SCAN_TIMEOUT_MS)
                .addDefaultDataViewParsers()
                .registerExtraResponseDataParser(
                        new ClarinFCSEndpointDescriptionParser())
                .enableLegacySupport()
                .buildThreadedClient();

        SRUThreadedClient sruSearchClient = new ClarinFCSClientBuilder()
                .setConnectTimeout(params.ENDPOINTS_SEARCH_TIMEOUT_MS)
                .setSocketTimeout(params.ENDPOINTS_SEARCH_TIMEOUT_MS)
                .addDefaultDataViewParsers()
                .registerExtraResponseDataParser(
                        new ClarinFCSEndpointDescriptionParser())
                .enableLegacySupport()
                .buildThreadedClient();

        maxScanConcurrentRequestsCallback = new MaxConcurrentRequestsCallback() {
            @Override
            public int getMaxConcurrentRequest(URI baseURI) {
                return params.SCAN_MAX_CONCURRENT_REQUESTS_PER_ENDPOINT;
            }
        };

        maxSearchConcurrentRequestsCallback = new MaxConcurrentRequestsCallback() {
            @Override
            public int getMaxConcurrentRequest(URI baseURI) {
                return (params.slowEndpoints != null && params.slowEndpoints.contains(baseURI))
                        ? params.SEARCH_MAX_CONCURRENT_REQUESTS_PER_SLOW_ENDPOINT
                        : params.SEARCH_MAX_CONCURRENT_REQUESTS_PER_ENDPOINT;
            }
        };

        sruClient = new ThrottledClient(
                sruScanClient, maxScanConcurrentRequestsCallback,
                sruSearchClient, maxSearchConcurrentRequestsCallback);

        File resourcesCacheFile = new File(params.AGGREGATOR_FILE_PATH);
        File resourcesOldCacheFile = new File(params.AGGREGATOR_FILE_PATH_BACKUP);

        // init resources from file
        {
            ObjectMapper mapper = new ObjectMapper()
                    .addMixIn(ClarinFCSEndpointDescription.DataView.class,
                            ClarinFCSEndpointDescriptionDataViewMixin.class)
                    .addMixIn(ClarinFCSEndpointDescription.Layer.class, ClarinFCSEndpointDescriptionLayerMixin.class);

            Resources resources = null;
            try {
                resources = mapper.readValue(resourcesCacheFile, Resources.class);
            } catch (Exception xc) {
                log.error("Failed to load cached resources from primary file:", xc);
            }
            if (resources == null) {
                try {
                    resources = mapper.readValue(resourcesOldCacheFile, Resources.class);
                } catch (Exception e) {
                    log.error("Failed to load cached resources from backup file:", e);
                }
            }
            if (resources != null) {
                scanCacheAtom.set(resources);
                log.info("resource list read from file; number of root resources: {}",
                        scanCacheAtom.get().getResources().size());
            }
        }

        LanguagesISO693.getInstance(); // force init
        initLanguageDetector();

        final Client jerseyClient = ClientFactory.create(CenterRegistryLive.CONNECT_TIMEOUT,
                CenterRegistryLive.READ_TIMEOUT, environment);

        ScanCrawlTask task = new ScanCrawlTask(sruClient, jerseyClient,
                params.CENTER_REGISTRY_URL, params.SCAN_MAX_DEPTH,
                params.additionalCQLEndpoints,
                params.additionalFCSEndpoints,
                null, scanCacheAtom, resourcesCacheFile, resourcesOldCacheFile,
                scanStatsAtom, searchStatsAtom);
        scheduler.scheduleAtFixedRate(task, params.SCAN_TASK_INITIAL_DELAY,
                params.SCAN_TASK_INTERVAL, params.getScanTaskTimeUnit());

        log.info("Aggregator initialization finished.");
    }

    // Definitions for Jackson Deserializer due to non-public non-default
    // constructors
    private static abstract class ClarinFCSEndpointDescriptionDataViewMixin {
        @SuppressWarnings("unused")
        ClarinFCSEndpointDescriptionDataViewMixin(@JsonProperty("identifier") String identifier,
                @JsonProperty("mimeType") String mimeType,
                @JsonProperty("deliveryPolicy") ClarinFCSEndpointDescription.DataView.DeliveryPolicy deliveryPolicy) {
        }
    }

    private static abstract class ClarinFCSEndpointDescriptionLayerMixin {
        @SuppressWarnings("unused")
        ClarinFCSEndpointDescriptionLayerMixin(@JsonProperty("identifier") String identifier,
                @JsonProperty("resultId") URI resultId, @JsonProperty("layerType") String layerType,
                @JsonProperty("encoding") ClarinFCSEndpointDescription.Layer.ContentEncoding encoding,
                @JsonProperty("qualifier") String qualifier, @JsonProperty("altValueInfo") String altValueInfo,
                @JsonProperty("altValueInfoURI") URI altValueInfoURI) {
        }
    }

    public void shutdown(AggregatorConfiguration config) {
        log.info("Aggregator is shutting down.");
        for (Search search : activeSearches.values()) {
            search.shutdown();
        }
        shutdownAndAwaitTermination(config.aggregatorParams, sruClient, scheduler);
        log.info("Aggregator shutdown complete.");
    }

    // this function should be thread-safe
    public Search startSearch(SRUVersion version, List<Resource> resources,
            String queryType, String searchString, String searchLang,
            int firstRecord, int maxRecords) throws Exception {
        if (resources.isEmpty()) {
            // No resources
            return null;
        } else if (searchString.isEmpty()) {
            // No query
            return null;
        } else {
            Search sr = new Search(sruClient,
                    version, searchStatsAtom.get(),
                    resources, queryType, searchString, searchLang, maxRecords);
            if (activeSearches.size() > SEARCHES_SIZE_GC_THRESHOLD) {
                List<Long> toBeRemoved = new ArrayList<Long>();
                long t0 = System.currentTimeMillis();
                for (Map.Entry<Long, Search> e : activeSearches.entrySet()) {
                    long dtmin = (t0 - e.getValue().getCreatedAt()) / 1000 / 60;
                    if (dtmin > SEARCHES_AGE_GC_THRESHOLD) {
                        log.info("removing search {}: {} minutes old", e.getKey(), dtmin);
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
