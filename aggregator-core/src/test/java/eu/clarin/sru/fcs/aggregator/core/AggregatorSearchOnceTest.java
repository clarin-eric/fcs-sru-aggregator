package eu.clarin.sru.fcs.aggregator.core;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import eu.clarin.sru.client.SRUVersion;
import eu.clarin.sru.fcs.aggregator.client.ThrottledClient;
import eu.clarin.sru.fcs.aggregator.scan.Endpoint;
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
        final SRUFCSClientParams sruClientParams = new SRUFCSClientParams() {
            @Override
            public int getEndpointScanTimeout() {
                return 3000;
            }

            @Override
            public int getEndpointSearchTimeout() {
                return 3000;
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
        };
        final ThrottledClient sruClient = AggregatorBase.createClient(sruClientParams, null);

        final Statistics stats = new Statistics();

        final Institution institution = new Institution("SAW Leipzig", null);
        final Endpoint endpoint = new Endpoint("https://fcs.data.saw-leipzig.de/lcc", FCSProtocolVersion.VERSION_2);
        final Resource resource = new Resource(institution, endpoint);
        resource.setHandle("hdl:11022/0000-0000-8F18-5");
        resource.setTitle("eng_news_2013_1M");

        final String query = "the";
        final Search search = AggregatorBase.startSearch(sruClient, stats, null, SRUVersion.VERSION_2_0,
                List.of(resource), "cql", query, null, 1, 10, null);
        final Result result = search.getResults(resource.getId()).get(0);

        log.info("Wait for search results ... (max 5s)");
        boolean finishedAwaiting = false;
        try {
            finishedAwaiting = result.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Awaiting of results interrupted", e);
        }
        log.info("Stopped waiting for search results. Is finished: {}. In progress: {}", finishedAwaiting,
                result.getInProgress());

        // shutdown search/client
        search.shutdown();
        sruClient.shutdown();

        log.info("Number of results for '{}': {}", query, result.getNumberOfRecordsLoaded());

        // log.info("Statistics:{}",aggregator.getSearchStatistics().getInstitutions());
        Statistics.EndpointStats epStats = stats.getInstitutions().get(institution.getName()).get(endpoint.getUrl());
        log.info("Statistics: {}", epStats);
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
