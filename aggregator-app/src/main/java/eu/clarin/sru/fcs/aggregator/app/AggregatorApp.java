package eu.clarin.sru.fcs.aggregator.app;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.servlet.DispatcherType;
import javax.ws.rs.client.Client;

import org.eclipse.jetty.server.session.SessionHandler;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import com.optimaize.langdetect.text.CommonTextObjectFactories;
import com.optimaize.langdetect.text.TextObjectFactory;

import de.mpg.aai.shhaa.AuthFilter;
import eu.clarin.sru.client.SRUVersion;
import eu.clarin.sru.client.fcs.ClarinFCSEndpointDescription;
import eu.clarin.sru.client.fcs.DataViewLex;
import eu.clarin.sru.fcs.aggregator.app.auth.AuthConfigContextListener;
import eu.clarin.sru.fcs.aggregator.app.auth.JWKSResource;
import eu.clarin.sru.fcs.aggregator.app.auth.LoginResource;
import eu.clarin.sru.fcs.aggregator.app.configuration.AAIConfig;
import eu.clarin.sru.fcs.aggregator.app.configuration.AggregatorConfiguration;
import eu.clarin.sru.fcs.aggregator.app.configuration.EndpointConfigImpl;
import eu.clarin.sru.fcs.aggregator.app.export.WeblichtExportCache;
import eu.clarin.sru.fcs.aggregator.app.rest.RestService;
import eu.clarin.sru.fcs.aggregator.app.serialization.AdvancedLayerSpanMixin;
import eu.clarin.sru.fcs.aggregator.app.serialization.ClarinFCSEndpointDescriptionDataViewMixin;
import eu.clarin.sru.fcs.aggregator.app.serialization.ClarinFCSEndpointDescriptionLayerMixin;
import eu.clarin.sru.fcs.aggregator.app.serialization.ClarinFCSEndpointDescriptionLexFieldMixin;
import eu.clarin.sru.fcs.aggregator.app.serialization.DataViewLexFieldMixin;
import eu.clarin.sru.fcs.aggregator.app.serialization.DataViewLexFieldTypeMixin;
import eu.clarin.sru.fcs.aggregator.app.serialization.DataViewLexMixin;
import eu.clarin.sru.fcs.aggregator.app.serialization.DataViewLexValueMixin;
import eu.clarin.sru.fcs.aggregator.app.serialization.DiagnosticRecordMixin;
import eu.clarin.sru.fcs.aggregator.app.serialization.InstitutionMixin;
import eu.clarin.sru.fcs.aggregator.app.serialization.ResourcesMixin;
import eu.clarin.sru.fcs.aggregator.app.serialization.ResultMetaMixin;
import eu.clarin.sru.fcs.aggregator.app.serialization.ResultMixin;
import eu.clarin.sru.fcs.aggregator.app.serialization.ResultRecordMixin;
import eu.clarin.sru.fcs.aggregator.app.serialization.StatisticsEndpointStatsMixin;
import eu.clarin.sru.fcs.aggregator.app.util.ClientFactory;
import eu.clarin.sru.fcs.aggregator.core.Aggregator;
import eu.clarin.sru.fcs.aggregator.core.AggregatorParams;
import eu.clarin.sru.fcs.aggregator.scan.CenterRegistryLive;
import eu.clarin.sru.fcs.aggregator.scan.EndpointConfig;
import eu.clarin.sru.fcs.aggregator.scan.Institution;
import eu.clarin.sru.fcs.aggregator.scan.Resource;
import eu.clarin.sru.fcs.aggregator.scan.Resources;
import eu.clarin.sru.fcs.aggregator.scan.ScanCrawlTask.ScanCrawlTaskCompletedCallback;
import eu.clarin.sru.fcs.aggregator.scan.Statistics;
import eu.clarin.sru.fcs.aggregator.search.AdvancedLayer;
import eu.clarin.sru.fcs.aggregator.search.DiagnosticRecord;
import eu.clarin.sru.fcs.aggregator.search.PerformLanguageDetectionCallback;
import eu.clarin.sru.fcs.aggregator.search.Result;
import eu.clarin.sru.fcs.aggregator.search.ResultMeta;
import eu.clarin.sru.fcs.aggregator.search.ResultRecord;
import eu.clarin.sru.fcs.aggregator.search.Search;
import eu.clarin.sru.fcs.aggregator.util.LanguagesISO693;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.views.common.ViewBundle;
import io.swagger.v3.jaxrs2.SwaggerSerializers;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;

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
public class AggregatorApp extends Application<AggregatorConfiguration> {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(AggregatorApp.class);

    public final static String NAME = "CLARIN FCS Aggregator";

    // aggregator core
    // TODO: does it need AtomicReference?
    private final static Aggregator aggregator = new Aggregator();
    private static AggregatorParams aggregatorParams;

    final int SEARCHES_SIZE_GC_THRESHOLD = 1000;
    final int SEARCHES_AGE_GC_THRESHOLD = 60;

    private static AggregatorApp instance;
    private AggregatorConfiguration.Params params;

    private AtomicReference<Resources> scanCacheAtom = new AtomicReference<Resources>(new Resources());
    private AtomicReference<Statistics> scanStatsAtom = new AtomicReference<Statistics>(new Statistics());

    private LanguageDetector languageDetector;
    private TextObjectFactory textObjectFactory;

    private Map<String, WeblichtExportCache> activeWeblichtExports = Collections.synchronizedMap(new HashMap<>());

    public static void main(String[] args) throws Exception {
        new AggregatorApp().run(args);
    }

    // ----------------------------------------------------------------------

    @Override
    public String getName() {
        return NAME;
    }

    public static AggregatorApp getInstance() {
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
        return aggregator.getSearchStatistics();
    }

    // ----------------------------------------------------------------------

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
            @SuppressWarnings("serial")
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
        for (String l : config.aggregatorParams.getWeblichtConfig().getAcceptedTcfLanguages()) {
            wll.add(LanguagesISO693.getInstance().code_3ForCode(l));
        }
        config.aggregatorParams.getWeblichtConfig().setAcceptedTcfLanguages(wll);

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

        // AAI - MPG SHHAA
        final AAIConfig aaiConfig = config.aggregatorParams.getAAIConfig();
        if (aaiConfig != null && aaiConfig.isAAIEnabled()) {
            // add resources (routes) for AAI and JWKS
            environment.jersey().register(new LoginResource());
            environment.jersey().register(new JWKSResource());

            // add AAI filters
            environment.servlets().addFilter("AAIFilter", AuthFilter.class)
                    .addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
            environment.servlets().setInitParameter("ShhaaConfigLocation", "/WEB-INF/shhaa.xml");
            // de.mpg.aai.shhaa.config.ConfigContextListener.ConfigContextListener
            environment.servlets().addServletListeners(new AuthConfigContextListener(aaiConfig.getShibWebappHost(),
                    aaiConfig.getShibLogin(), aaiConfig.getShibLogout()));
        }

        // pretty printing
        if (config.aggregatorParams.getPrettyPrintJSON()) {
            environment.getObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true);
        }

        // custom serialization of POJOs
        environment.getObjectMapper()
                // rename fields, block serialization of empty fields
                .addMixIn(DataViewLex.class, DataViewLexMixin.class)
                // required?
                .addMixIn(DataViewLex.FieldType.class, DataViewLexFieldTypeMixin.class)
                // required?
                .addMixIn(DataViewLex.Field.class, DataViewLexFieldMixin.class)
                // block serialization of empty/unchanged fields
                .addMixIn(DataViewLex.Value.class, DataViewLexValueMixin.class)
                // rename fields
                .addMixIn(ClarinFCSEndpointDescription.LexField.class, ClarinFCSEndpointDescriptionLexFieldMixin.class)
                // define what to serialize
                .addMixIn(Statistics.EndpointStats.class, StatisticsEndpointStatsMixin.class)
                // block serialization of unset field
                .addMixIn(Institution.class, InstitutionMixin.class)
                // rename fields
                .addMixIn(ResultMeta.class, ResultMetaMixin.class)
                // block serialization of unwanted fields
                //.addMixIn(Result.class, ResultMixin.class) // TODO: for testing, so that UI does not crash
                // rename fields
                .addMixIn(ResultRecord.class, ResultRecordMixin.class)
                .addMixIn(DiagnosticRecord.class, DiagnosticRecordMixin.class)
                // expose fields
                .addMixIn(AdvancedLayer.Span.class, AdvancedLayerSpanMixin.class);

        // swagger
        if (config.aggregatorParams.isOpenAPIEnabled()) {
            List<String> resourceClasses = new ArrayList<>(List.of("eu.clarin.sru.fcs.aggregator.app.IndexResource",
                    "eu.clarin.sru.fcs.aggregator.app.rest.RestService"));

            if (aaiConfig != null && aaiConfig.isAAIEnabled()) {
                resourceClasses.addAll(List.of("eu.clarin.sru.fcs.aggregator.app.auth.LoginResource",
                        "eu.clarin.sru.fcs.aggregator.app.auth.JWKSResource"));
            }
            final SwaggerConfiguration oasConfiguration = new SwaggerConfiguration()
                    .openAPI(new OpenAPI().addServersItem(
                            new Server().url(config.aggregatorParams.getSERVER_URL())
                                    .description("Local API endpoint")))
                    .prettyPrint(true)
                    .readAllResources(true)
                    .resourceClasses(Set.copyOf(resourceClasses));
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

    public void init(Environment environment) throws IOException {
        log.info("Aggregator initialization started.");

        // make aggregator core params
        aggregatorParams = new AggregatorParams() {
            @Override
            public int getEndpointScanTimeout() {
                return params.getENDPOINTS_SCAN_TIMEOUT_MS();
            }

            @Override
            public int getEndpointSearchTimeout() {
                return params.getENDPOINTS_SEARCH_TIMEOUT_MS();
            }

            @Override
            public int getMaxConcurrentScanRequestsPerEndpoint() {
                return params.getSCAN_MAX_CONCURRENT_REQUESTS_PER_ENDPOINT();
            }

            @Override
            public int getMaxConcurrentSearchRequestsPerEndpoint() {
                return params.getSEARCH_MAX_CONCURRENT_REQUESTS_PER_ENDPOINT();
            }

            @Override
            public int getMaxConcurrentSearchRequestsPerSlowEndpoint() {
                return params.getSEARCH_MAX_CONCURRENT_REQUESTS_PER_SLOW_ENDPOINT();
            }

            @Override
            public List<URI> getSlowEndpoints() {
                return params.getSlowEndpoints();
            }

            @Override
            public String getCenterRegistryUrl() {
                return params.getCENTER_REGISTRY_URL();
            }

            @Override
            public int getScanMaxDepth() {
                return params.getSCAN_MAX_DEPTH();
            }

            @Override
            public List<EndpointConfig> getAdditionalCQLEndpoints() {
                List<EndpointConfigImpl> endpoints = params.getAdditionalCQLEndpoints();
                if (endpoints == null) {
                    return null;
                }
                return endpoints.stream().map(e -> (EndpointConfig) e).collect(Collectors.toList());
            }

            @Override
            public List<EndpointConfig> getAdditionalFCSEndpoints() {
                List<EndpointConfigImpl> endpoints = params.getAdditionalFCSEndpoints();
                if (endpoints == null) {
                    return null;
                }
                return endpoints.stream().map(e -> (EndpointConfig) e).collect(Collectors.toList());
            }

            @Override
            public long getScanTaskInitialDelay() {
                return params.getSCAN_TASK_INITIAL_DELAY();
            }

            @Override
            public long getScanTaskInterval() {
                return params.getSCAN_TASK_INTERVAL();
            }

            @Override
            public TimeUnit getScanTaskTimeUnit() {
                return params.getScanTaskTimeUnit();
            }

            @Override
            public long getExecutorShutdownTimeout() {
                return params.getEXECUTOR_SHUTDOWN_TIMEOUT_MS();
            }

            @Override
            public int getSearchesSizeThreshold() {
                return SEARCHES_SIZE_GC_THRESHOLD;
            }

            @Override
            public int getSearchesAgeThreshold() {
                return SEARCHES_AGE_GC_THRESHOLD;
            }

            @Override
            public boolean enableScanCrawlTask() {
                return true;
            }

            @Override
            public boolean enableAAI() {
                final AAIConfig aaiConfig = params.getAAIConfig();
                return (aaiConfig != null) ? aaiConfig.isAAIEnabled() : false;
            }

            @Override
            public String getServerUrl() {
                return params.getSERVER_URL();
            }

            @Override
            public String getPublicKey() {
                final AAIConfig aaiConfig = params.getAAIConfig();
                if (aaiConfig == null) {
                    return null;
                }
                final AAIConfig.KeyConfig key = aaiConfig.getKey();
                if (key == null) {
                    return null;
                }
                return key.getPublicKey();
            }

            @Override
            public String getPrivateKey() {
                final AAIConfig aaiConfig = params.getAAIConfig();
                if (aaiConfig == null) {
                    return null;
                }
                final AAIConfig.KeyConfig key = aaiConfig.getKey();
                if (key == null) {
                    return null;
                }
                return key.getPrivateKey();
            }
        };

        // cached resources loading
        final File resourcesCacheFile = new File(params.getAGGREGATOR_FILE_PATH());
        final File resourcesOldCacheFile = new File(params.getAGGREGATOR_FILE_PATH_BACKUP());

        // init resources from file
        {
            Resources resources = loadResourcesCache(resourcesCacheFile, resourcesOldCacheFile);
            if (resources != null) {
                scanCacheAtom.set(resources);
            }
        }

        // force init
        LanguagesISO693.getInstance();
        // {
        // System.out.println("LanguagesISO693: ");
        // final ObjectWriter ow = new ObjectMapper().writerWithDefaultPrettyPrinter();
        // try {
        // System.out.println(ow.writeValueAsString(LanguagesISO693.getInstance().getCodeToLangMap()));
        // } catch (JsonProcessingException ex) {
        // }
        // }

        initLanguageDetector();

        // inject language detection for search results
        final PerformLanguageDetectionCallback performLanguageDetectionCallback = new PerformLanguageDetectionCallback() {
            @Override
            public String detect(String content) {
                String code_iso639_1 = detectLanguage(content);
                String language = code_iso639_1 == null ? null
                        : LanguagesISO693.getInstance().code_3ForCode(code_iso639_1);
                return language;
            }
        };

        // client for CLARIN registry
        final Client jerseyClient = ClientFactory.create(CenterRegistryLive.CONNECT_TIMEOUT,
                CenterRegistryLive.READ_TIMEOUT, environment);

        final ScanCrawlTaskCompletedCallback scanCrawlTaskCompletedCallback = new ScanCrawlTaskCompletedCallback() {
            @Override
            public void onSuccess(Resources resources, Statistics statistics) {
                if (resources.getResources().isEmpty()) {
                    log.warn("ScanCrawlTask: No resources: skipped updating stats; skipped writing to disk.");
                } else {
                    scanCacheAtom.set(resources);
                    scanStatsAtom.set(statistics);
                    aggregator.setSearchStatistics(new Statistics()); // reset search stats

                    try {
                        writeResourcesCache(resources, resourcesCacheFile, resourcesOldCacheFile);
                    } catch (IOException xc) {
                        log.error("ScanCrawlTask: error writing resources cache file", xc);
                    }

                    log.info("ScanCrawlTask: wrote to disk, finished");
                }
            }

            @Override
            public void onError(Throwable xc) {
                log.error("ScanCrawlTask: exception", xc);
            }
        };

        aggregator.setPerformLanguageDetectionCallback(performLanguageDetectionCallback);
        aggregator.init(jerseyClient, aggregatorParams, null, scanCrawlTaskCompletedCallback);

        log.info("Aggregator initialization finished.");
    }

    // ----------------------------------------------------------------------

    public void shutdown(AggregatorConfiguration config) {
        log.info("Aggregator is shutting down.");
        aggregator.shutdown(aggregatorParams);
        log.info("Aggregator shutdown complete.");
    }

    // ----------------------------------------------------------------------

    // this function should be thread-safe
    public Search startSearch(SRUVersion version, List<Resource> resources,
            String queryType, String searchString, String searchLang,
            int startRecord, int maxRecords, String userid) throws Exception {
        // first some cleanup
        List<String> prunedSearchIds = aggregator.gcSearches(aggregatorParams);
        for (String searchId : prunedSearchIds) {
            activeWeblichtExports.remove(searchId);
        }

        // then the search
        return aggregator.startSearch(version, resources, queryType, searchString, searchLang, startRecord, maxRecords,
                userid);
    }

    public Search getSearchById(String id) {
        return aggregator.getSearchById(id);
    }

    public WeblichtExportCache getWeblichtExportCacheBySearchId(String searchId, boolean createIfNeeded) {
        WeblichtExportCache cache = activeWeblichtExports.get(searchId);
        if (cache == null && createIfNeeded) {
            cache = new WeblichtExportCache();
            activeWeblichtExports.put(searchId, cache);
        }
        return cache;
    }

    // ----------------------------------------------------------------------

    public void initLanguageDetector() throws IOException {
        List<LanguageProfile> languageProfiles = new LanguageProfileReader().readAllBuiltIn();
        languageDetector = LanguageDetectorBuilder
                .create(NgramExtractors.standard())
                .withProfiles(languageProfiles)
                .build();

        textObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText();
    }

    public String detectLanguage(String text) {
        LdLocale lang = languageDetector.detect(textObjectFactory.forText(text)).orNull();
        if (lang != null) {
            return lang.getLanguage();
        }
        return null;
    }

    // ----------------------------------------------------------------------

    private static ObjectMapper createResourcesCacheMapper() {
        // add definitions
        // - for Jackson Deserializer due to non-public non-default constructors
        // - ignore unknown properties
        return new ObjectMapper()
                .addMixIn(ClarinFCSEndpointDescription.DataView.class,
                        ClarinFCSEndpointDescriptionDataViewMixin.class)
                .addMixIn(ClarinFCSEndpointDescription.Layer.class, ClarinFCSEndpointDescriptionLayerMixin.class)
                .addMixIn(ClarinFCSEndpointDescription.LexField.class, ClarinFCSEndpointDescriptionLexFieldMixin.class)
                .addMixIn(Resources.class, ResourcesMixin.class)
                .addMixIn(Institution.class, InstitutionMixin.class)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private static void writeResourcesCache(Resources resources, File cachedResources, File oldCachedResources)
            throws IOException {
        if (cachedResources.exists()) {
            try {
                oldCachedResources.delete();
            } catch (Throwable txc) {
                // ignore
            }
            try {
                cachedResources.renameTo(oldCachedResources);
            } catch (Throwable txc) {
                // ignore
            }
        }
        ObjectMapper mapper = createResourcesCacheMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(cachedResources, resources);
    }

    private static Resources loadResourcesCache(File cachedResources, File oldCachedResources) throws IOException {
        ObjectMapper mapper = createResourcesCacheMapper();

        Resources resources = null;
        try {
            resources = mapper.readValue(cachedResources, Resources.class);
        } catch (Exception xc) {
            log.error("Failed to load cached resources from primary file:", xc);
        }
        if (resources == null) {
            try {
                resources = mapper.readValue(oldCachedResources, Resources.class);
            } catch (Exception e) {
                log.error("Failed to load cached resources from backup file:", e);
            }
        }
        if (resources != null) {
            log.info("Resource list read from file. Number of root resources: {}", resources.getResources().size());
        }
        return resources;
    }

}
