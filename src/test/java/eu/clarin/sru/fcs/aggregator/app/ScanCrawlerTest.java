package eu.clarin.sru.fcs.aggregator.app;

import eu.clarin.sru.client.SRUThreadedClient;
import eu.clarin.sru.client.fcs.ClarinFCSClientBuilder;
import static eu.clarin.sru.fcs.aggregator.app.CQLEnumerationTest.RULE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.clarin.sru.fcs.aggregator.client.MaxConcurrentRequestsCallback;
import eu.clarin.sru.fcs.aggregator.scan.Corpora;
import eu.clarin.sru.fcs.aggregator.scan.EndpointUrlFilterAllow;
import eu.clarin.sru.fcs.aggregator.scan.ScanCrawler;
import eu.clarin.sru.fcs.aggregator.client.ThrottledClient;
import eu.clarin.sru.fcs.aggregator.scan.CenterRegistryLive;
import eu.clarin.sru.fcs.aggregator.scan.Corpus;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardClientExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;

/**
 *
 * @author yanapanchenko
 */
@Disabled("Live tests only manually")
@ExtendWith(DropwizardExtensionsSupport.class)
public class ScanCrawlerTest {

    static DropwizardResourceConfig x = new DropwizardResourceConfig();

    @RegisterExtension
    private static final DropwizardAppExtension<AggregatorConfiguration> RULE = new DropwizardAppExtension<>(
            Aggregator.class, ResourceHelpers.resourceFilePath("aggregator_test.yml"));

    @Test
    public void testCrawlForMpiAndTue() throws NamingException {
        Environment env = RULE.getEnvironment();
        env.metrics().removeMatching(new MetricFilter() {
            @Override
            public boolean matches(String name, Metric metric) {
                return true;
            }
        });

        SRUThreadedClient sruThreadedClient = new ClarinFCSClientBuilder()
                .addDefaultDataViewParsers()
                .buildThreadedClient();
        MaxConcurrentRequestsCallback callback = new MaxConcurrentRequestsCallback() {
            @Override
            public int getMaxConcurrentRequest(URI baseURI) {
                return 2;
            }
        };
        ThrottledClient sruClient = new ThrottledClient(
                sruThreadedClient, callback,
                sruThreadedClient, callback);

        try {
            EndpointUrlFilterAllow filter = new EndpointUrlFilterAllow("uni-tuebingen.de");
            // , "leipzig", ".mpi.nl", "dspin.dwds.de", "lindat."

            // InitialContext context = new InitialContext();
            // String centerRegistryUrl = (String)
            // context.lookup("java:comp/env/center-registry-url");
            String centerRegistryUrl = RULE.getConfiguration().aggregatorParams.CENTER_REGISTRY_URL;
            ScanCrawler crawler = new ScanCrawler(
                    new CenterRegistryLive(centerRegistryUrl, filter, env).getCQLInstitutions(),
                    sruClient, 2);
            Corpora cache = crawler.crawl();
            Corpus tueRootCorpus = cache.findByEndpoint("http://weblicht.sfs.uni-tuebingen.de/rws/sru/").get(0);
            Corpus mpiRootCorpus = cache.findByEndpoint("http://cqlservlet.mpi.nl/").get(0);
            assertEquals("http://hdl.handle.net/11858/00-1778-0000-0001-DDAF-D",
                    tueRootCorpus.getHandle());
            Corpus mpiCorpus = cache.findByHandle("hdl:1839/00-0000-0000-0001-53A5-2@format=cmdi");
            assertEquals("hdl:1839/00-0000-0000-0003-4692-D@format=cmdi",
                    mpiCorpus.getSubCorpora().get(0).getHandle());
            // check if languages and other corpus data is crawled corectly...
            Set<String> tueLangs = new HashSet<>();
            tueLangs.add("deu");
            assertEquals(tueLangs, tueRootCorpus.getLanguages());
            String tueDescSubstring = "Tübingen Treebank";
            assertTrue(tueRootCorpus.getDescription().contains(tueDescSubstring), "Description problem");
            String tueNameSubstring = "TuebaDDC";
            assertTrue(tueRootCorpus.getTitle().contains(tueNameSubstring), "Name problem");
            String tuePageSubstring = "sfs.uni-tuebingen.de";
            assertTrue(tueRootCorpus.getLandingPage().contains(tuePageSubstring), "Landing page problem");
            assertTrue(mpiRootCorpus.getNumberOfRecords() > 10, "Number of records problem");

        } finally {
            sruClient.shutdown();
        }
    }
}
