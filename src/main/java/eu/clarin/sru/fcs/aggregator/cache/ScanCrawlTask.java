package eu.clarin.sru.fcs.aggregator.cache;

import eu.clarin.sru.client.SRUThreadedClient;
import eu.clarin.sru.fcs.aggregator.registry.CenterRegistryLive;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.LoggerFactory;

/**
 * A task for crawling endpoint scan operation responses of FCS specification.
 * If successful, saves found endpoints and resources descriptions into a new
 * ScanCache and updates the web application contexts with this new cache, as
 * well as rewrites the previously scanned data saved on the disk.
 *
 * @author yanapanchenko
 * @author edima
 */
public class ScanCrawlTask implements Runnable {
	
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(ScanCrawlTask.class);
	
	private SRUThreadedClient sruClient;
	private ScanCachePersistence scanCachePersistence;
	private AtomicReference<ScanCache> scanCacheAtom;
	private int cacheMaxDepth;
	private EndpointFilter filter;
	
	public ScanCrawlTask(SRUThreadedClient sruClient, int cacheMaxDepth, EndpointFilter filter,
			ScanCachePersistence scanCachePersistence, AtomicReference<ScanCache> scanCacheAtom) {
		this.sruClient = sruClient;
		this.cacheMaxDepth = cacheMaxDepth;
		this.filter = filter;
		this.scanCachePersistence = scanCachePersistence;
		this.scanCacheAtom = scanCacheAtom;
	}
	
	@Override
	public void run() {
		try {
			log.info("STARTING CACHING CORPORA SCAN");
			long time0 = System.currentTimeMillis();
			
			ScanCrawler scanCrawler = new ScanCrawler(new CenterRegistryLive(), sruClient, filter, cacheMaxDepth);
			ScanCache cache = scanCrawler.crawl();
			
			log.info("New Cache, number of root corpora: " + cache.getRootCorpora().size());
			scanCachePersistence.write(cache);
			scanCacheAtom.set(cache);
			long time = System.currentTimeMillis() - time0;
			
			log.info("FINISHED CACHING CORPORA SCAN ({}s)", time / 1000.);
		} catch (Exception xc) {
			log.error("!!! Scan Crawler task exception", xc);
		}
	}
}
