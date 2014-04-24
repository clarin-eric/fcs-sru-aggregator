package eu.clarin.sru.fcs.aggregator.app;

import eu.clarin.sru.client.SRUThreadedClient;
import eu.clarin.sru.fcs.aggregator.sopt.CenterRegistryLive;
import eu.clarin.sru.fcs.aggregator.sopt.Corpus;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author yanapanchenko
 */
public class ScanCrawlerTest {

    @Test
    public void testCrawlForMpiAndTue() {

//        SRUThreadedClient sruClient = new SRUThreadedClient();
//
//        try {
//            EndpointUrlFilter filter = new EndpointUrlFilter();
//            filter.urlShouldContainAnyOf("uni-tuebingen.de", "mpi.nl");
//            ScanCrawler crawler = new ScanCrawler(new CenterRegistryLive(), sruClient, filter, 2);
//            ScanCache cache = crawler.crawl();
//            Corpus tueRootCorpus = cache.getRootCorporaOfEndpoint("http://weblicht.sfs.uni-tuebingen.de/rws/sru/").get(0);
//            Corpus mpiRootCorpus = cache.getRootCorporaOfEndpoint("http://cqlservlet.mpi.nl/").get(0);
//            Assert.assertEquals("http://hdl.handle.net/11858/00-1778-0000-0001-DDAF-D",
//                    tueRootCorpus.getHandle());
//            Assert.assertEquals("hdl:1839/00-0000-0000-0003-4692-D@format=cmdi",
//                    cache.getChildrenCorpora("hdl:1839/00-0000-0000-0001-53A5-2@format=cmdi").get(0).getHandle());
//            //check if languages and other corpus data is crawled corectly...
//            Set<String> tueLangs = new HashSet<String>();
//            tueLangs.add("deu");
//            Assert.assertEquals(tueLangs, tueRootCorpus.getLanguages());
//            String tueDescSubstring = "Tübingen Treebank";
//            Assert.assertTrue("Description problem", tueRootCorpus.getDescription().contains(tueDescSubstring));
//            String tueNameSubstring = "TuebaDDC";
//            Assert.assertTrue("Name problem", tueRootCorpus.getDisplayName().contains(tueNameSubstring));
//            String tuePageSubstring = "sfs.uni-tuebingen.de";
//            Assert.assertTrue("Landing page problem", tueRootCorpus.getLandingPage().contains(tuePageSubstring));
//            Assert.assertTrue("Number of records problem", mpiRootCorpus.getNumberOfRecords() > 10);
//
//        } finally {
//            sruClient.shutdown();
//        }
//
    }
}
