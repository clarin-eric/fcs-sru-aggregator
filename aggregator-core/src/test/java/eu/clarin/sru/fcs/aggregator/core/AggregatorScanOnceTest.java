package eu.clarin.sru.fcs.aggregator.core;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import eu.clarin.sru.fcs.aggregator.client.ThrottledClient;
import eu.clarin.sru.fcs.aggregator.scan.EndpointConfig;
import eu.clarin.sru.fcs.aggregator.scan.Resources;
import eu.clarin.sru.fcs.aggregator.scan.ScanCrawlTask;
import eu.clarin.sru.fcs.aggregator.scan.ScanCrawlTask.ScanCrawlTaskCompletedCallback;
import eu.clarin.sru.fcs.aggregator.scan.Statistics;

@Disabled("Live tests only manually")
public class AggregatorScanOnceTest {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(AggregatorScanOnceTest.class);

    @Test
    public void testScanTaskWithCLARINCentreRegistryOnce() throws InterruptedException {
        final Client jerseyClient = ClientBuilder.newClient();

        final SRUFCSClientParams sruClientParams = new SRUFCSClientParams() {
            @Override
            public int getEndpointScanTimeout() {
                return 60000;
            }

            @Override
            public int getEndpointSearchTimeout() {
                return 30000;
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
        final ThrottledClient sruClient = AggregatorBase.createClient(sruClientParams);

        final ScanCrawlTaskCompletedCallback scanCrawlTaskCompletedCallback = new ScanCrawlTaskCompletedCallback() {
            @Override
            public void onSuccess(Resources resources, Statistics statistics) {
                System.out.println("Resources: " + resources.getResources().size());
            }

            @Override
            public void onError(Throwable xc) {
                System.err.println("Exception: " + xc.getMessage());
            }

        };

        final ScanCrawlParams scanScrawlTaskParams = new ScanCrawlParams() {
            @Override
            public String getCenterRegistryUrl() {
                return "https://centres.clarin.eu/restxml/";
            }

            @Override
            public int getScanMaxDepth() {
                return 1;
            }

            @Override
            public List<EndpointConfig> getAdditionalCQLEndpoints() {
                return null;
            }

            @Override
            public List<EndpointConfig> getAdditionalFCSEndpoints() {
                return null;
            }

            @Override
            public long getScanTaskInitialDelay() {
                return 0;
            }

            @Override
            public long getScanTaskInterval() {
                return 12;
            }

            @Override
            public TimeUnit getScanTaskTimeUnit() {
                return TimeUnit.HOURS;
            }
        };

        final ScanCrawlTask task = AggregatorBase.createScanCrawlTask(sruClient, jerseyClient, scanScrawlTaskParams,
                null, scanCrawlTaskCompletedCallback);
        log.info("Start ScanCrawlTask ...");
        task.run();
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
