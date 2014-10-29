package eu.clarin.sru.fcs.aggregator.app;

import eu.clarin.sru.client.SRUThreadedClient;
import eu.clarin.sru.client.fcs.ClarinFCSClientBuilder;
import eu.clarin.sru.fcs.aggregator.cache.EndpointUrlFilter;
import eu.clarin.sru.fcs.aggregator.cache.ScanCache;
import eu.clarin.sru.fcs.aggregator.cache.ScanCrawler;
import eu.clarin.sru.fcs.aggregator.registry.CenterRegistryLive;
import eu.clarin.sru.fcs.aggregator.registry.Corpus;
import java.util.HashSet;
import java.util.Set;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author yanapanchenko
 */
@Ignore
public class ScanCrawlerTest {

	@Test
	public void testCrawlForMpiAndTue() {

		SRUThreadedClient sruClient = new ClarinFCSClientBuilder()
				.addDefaultDataViewParsers()
				.buildThreadedClient();

		try {
			EndpointUrlFilter filter = new EndpointUrlFilter()
					.allow("uni-tuebingen.de"); //, "leipzig", ".mpi.nl", "dspin.dwds.de", "lindat."
			ScanCrawler crawler = new ScanCrawler(new CenterRegistryLive(), sruClient, filter, 2);
			ScanCache cache = crawler.crawl();
			Corpus tueRootCorpus = cache.getRootCorporaOfEndpoint("http://weblicht.sfs.uni-tuebingen.de/rws/sru/").get(0);
			Corpus mpiRootCorpus = cache.getRootCorporaOfEndpoint("http://cqlservlet.mpi.nl/").get(0);
			Assert.assertEquals("http://hdl.handle.net/11858/00-1778-0000-0001-DDAF-D",
					tueRootCorpus.getHandle());
			Corpus mpiCorpus = cache.getCorpus("hdl:1839/00-0000-0000-0001-53A5-2@format=cmdi");
			Assert.assertEquals("hdl:1839/00-0000-0000-0003-4692-D@format=cmdi", cache.getChildren(mpiCorpus).get(0).getHandle());
			//check if languages and other corpus data is crawled corectly...
			Set<String> tueLangs = new HashSet<>();
			tueLangs.add("deu");
			Assert.assertEquals(tueLangs, tueRootCorpus.getLanguages());
			String tueDescSubstring = "Tübingen Treebank";
			Assert.assertTrue("Description problem", tueRootCorpus.getDescription().contains(tueDescSubstring));
			String tueNameSubstring = "TuebaDDC";
			Assert.assertTrue("Name problem", tueRootCorpus.getDisplayName().contains(tueNameSubstring));
			String tuePageSubstring = "sfs.uni-tuebingen.de";
			Assert.assertTrue("Landing page problem", tueRootCorpus.getLandingPage().contains(tuePageSubstring));
			Assert.assertTrue("Number of records problem", mpiRootCorpus.getNumberOfRecords() > 10);

		} finally {
			sruClient.shutdown();
		}
	}
}
