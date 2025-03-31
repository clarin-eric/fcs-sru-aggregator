package eu.clarin.sru.fcs.aggregator.core;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import eu.clarin.sru.client.SRUVersion;
import eu.clarin.sru.fcs.aggregator.scan.Endpoint;
import eu.clarin.sru.fcs.aggregator.scan.EndpointConfig;
import eu.clarin.sru.fcs.aggregator.scan.FCSProtocolVersion;
import eu.clarin.sru.fcs.aggregator.scan.Institution;
import eu.clarin.sru.fcs.aggregator.scan.Resource;
import eu.clarin.sru.fcs.aggregator.scan.Statistics;
import eu.clarin.sru.fcs.aggregator.search.Result;
import eu.clarin.sru.fcs.aggregator.search.Search;

@Disabled("Live tests only manually")
public class AggregatorSearchOnceTest {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(AggregatorSearchOnceTest.class);

    @Test
    public void testSearchOnce() throws Exception {
        final Client jerseyClient = ClientBuilder.newClient();

        final AggregatorParams params = new AggregatorParams() {
            @Override
            public int getEndpointScanTimeout() {
                return 1000;
            }

            @Override
            public int getEndpointSearchTimeout() {
                return 5000;
            }

            @Override
            public int getMaxConcurrentScanRequestsPerEndpoint() {
                return 4;
            }

            @Override
            public int getMaxConcurrentSearchRequestsPerEndpoint() {
                return 4;
            }

            @Override
            public int getMaxConcurrentSearchRequestsPerSlowEndpoint() {
                return 1;
            }

            @Override
            public List<URI> getSlowEndpoints() {
                return null;
            }

            @Override
            public String getCenterRegistryUrl() {
                throw new UnsupportedOperationException("Unimplemented method 'getCenterRegistryUrl'");
            }

            @Override
            public int getScanMaxDepth() {
                throw new UnsupportedOperationException("Unimplemented method 'getScanMaxDepth'");
            }

            @Override
            public List<EndpointConfig> getAdditionalCQLEndpoints() {
                throw new UnsupportedOperationException("Unimplemented method 'getAdditionalCQLEndpoints'");
            }

            @Override
            public List<EndpointConfig> getAdditionalFCSEndpoints() {
                throw new UnsupportedOperationException("Unimplemented method 'getAdditionalFCSEndpoints'");
            }

            @Override
            public long getScanTaskInitialDelay() {
                throw new UnsupportedOperationException("Unimplemented method 'getScanTaskInitialDelay'");
            }

            @Override
            public long getScanTaskInterval() {
                throw new UnsupportedOperationException("Unimplemented method 'getScanTaskInterval'");
            }

            @Override
            public TimeUnit getScanTaskTimeUnit() {
                throw new UnsupportedOperationException("Unimplemented method 'getScanTaskTimeUnit'");
            }

            @Override
            public long getExecutorShutdownTimeout() {
                throw new UnsupportedOperationException("Unimplemented method 'getExecutorShutdownTimeout'");
            }

            @Override
            public int getSearchesSizeThreshold() {
                return 1000;
            }

            @Override
            public int getSearchesAgeThreshold() {
                return 50;
            }

            @Override
            public boolean enableScanCrawlTask() {
                return false;
            }

        };
        final Aggregator aggregator = new Aggregator();
        aggregator.init(jerseyClient, params, null, null);

        final Institution institution = new Institution("SAW Leipzig", null);
        final Endpoint endpoint = new Endpoint("https://fcs.data.saw-leipzig.de/lcc", FCSProtocolVersion.VERSION_2);
        final Resource resource = new Resource(institution, endpoint);
        resource.setHandle("hdl:11022/0000-0000-8F18-5");
        resource.setTitle("eng_news_2013_1M");

        final String query = "the";
        final Search search = aggregator.startSearch(SRUVersion.VERSION_2_0, List.of(resource), "cql", query, null, 1,
                10);
        final Result result = search.getResults(resource.getId()).get(0);

        log.info("Wait for search results ... (max 3s)");
        for (int i = 0; i < 30; i++) {
            if (!result.getInProgress()) {
                break;
            }
            Thread.sleep(100);
        }

        log.info("Number of results for '{}': {}", query, result.getNumberOfRecordsLoaded());

        // log.info("Statistics:{}",aggregator.getSearchStatistics().getInstitutions());
        Statistics.EndpointStats stats = aggregator.getSearchStatistics()
                .getInstitutions()
                .get(institution.getName())
                .get(endpoint.getUrl());
        log.info("Statistics: {}", stats);
    }

    static {
        org.apache.log4j.BasicConfigurator
                .configure(new org.apache.log4j.ConsoleAppender(
                        new org.apache.log4j.PatternLayout("%-5p [%c{1}][%t] %m%n"),
                        org.apache.log4j.ConsoleAppender.SYSTEM_ERR));
        org.apache.log4j.Logger logger = org.apache.log4j.Logger.getRootLogger();
        logger.setLevel(org.apache.log4j.Level.INFO);
        logger.getLoggerRepository().getLogger("eu.clarin").setLevel(org.apache.log4j.Level.DEBUG);
    }
}
