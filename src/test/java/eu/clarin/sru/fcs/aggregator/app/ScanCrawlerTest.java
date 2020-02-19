package eu.clarin.sru.fcs.aggregator.app;

import eu.clarin.sru.client.SRUThreadedClient;
import eu.clarin.sru.client.fcs.ClarinFCSClientBuilder;
import static eu.clarin.sru.fcs.aggregator.app.CQLEnumerationTest.RULE;
import eu.clarin.sru.fcs.aggregator.client.MaxConcurrentRequestsCallback;
import eu.clarin.sru.fcs.aggregator.scan.Corpora;
import eu.clarin.sru.fcs.aggregator.scan.EndpointUrlFilterAllow;
import eu.clarin.sru.fcs.aggregator.scan.ScanCrawler;
import eu.clarin.sru.fcs.aggregator.client.ThrottledClient;
import eu.clarin.sru.fcs.aggregator.scan.CenterRegistryLive;
import eu.clarin.sru.fcs.aggregator.scan.Corpus;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author yanapanchenko
 */
@Ignore
public class ScanCrawlerTest {

    @ClassRule
    public static final DropwizardAppRule<AggregatorConfiguration> RULE =
            new DropwizardAppRule<>(Aggregator.class, ResourceHelpers.resourceFilePath("aggregator_devel.yaml"));
    
	@Test
	public void testCrawlForMpiAndTue() throws NamingException {
                Environment env = RULE.getEnvironment();
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
				sruThreadedClient, callback
		);

		try {
			EndpointUrlFilterAllow filter = new EndpointUrlFilterAllow("uni-tuebingen.de"); //, "leipzig", ".mpi.nl", "dspin.dwds.de", "lindat."

			InitialContext context = new InitialContext();
			String centerRegistryUrl = (String) context.lookup("java:comp/env/center-registry-url");
			ScanCrawler crawler = new ScanCrawler(
					new CenterRegistryLive(centerRegistryUrl, filter, env).getCQLInstitutions(),
					sruClient, 2);
			Corpora cache = crawler.crawl();
			Corpus tueRootCorpus = cache.findByEndpoint("http://weblicht.sfs.uni-tuebingen.de/rws/sru/").get(0);
			Corpus mpiRootCorpus = cache.findByEndpoint("http://cqlservlet.mpi.nl/").get(0);
			Assert.assertEquals("http://hdl.handle.net/11858/00-1778-0000-0001-DDAF-D",
					tueRootCorpus.getHandle());
			Corpus mpiCorpus = cache.findByHandle("hdl:1839/00-0000-0000-0001-53A5-2@format=cmdi");
			Assert.assertEquals("hdl:1839/00-0000-0000-0003-4692-D@format=cmdi", mpiCorpus.getSubCorpora().get(0).getHandle());
			//check if languages and other corpus data is crawled corectly...
			Set<String> tueLangs = new HashSet<>();
			tueLangs.add("deu");
			Assert.assertEquals(tueLangs, tueRootCorpus.getLanguages());
			String tueDescSubstring = "Tübingen Treebank";
			Assert.assertTrue("Description problem", tueRootCorpus.getDescription().contains(tueDescSubstring));
			String tueNameSubstring = "TuebaDDC";
			Assert.assertTrue("Name problem", tueRootCorpus.getTitle().contains(tueNameSubstring));
			String tuePageSubstring = "sfs.uni-tuebingen.de";
			Assert.assertTrue("Landing page problem", tueRootCorpus.getLandingPage().contains(tuePageSubstring));
			Assert.assertTrue("Number of records problem", mpiRootCorpus.getNumberOfRecords() > 10);

		} finally {
			sruClient.shutdown();
		}
	}
}
