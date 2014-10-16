package eu.clarin.sru.fcs.aggregator.cache;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A task for crawling endpoint scan operation responses of FCS specification.
 * If successful, saves found endpoints and resources descriptions into a new 
 * ScanCache and updates the web application contexts with this new cache, as 
 * well as rewrites the previously scanned data saved on the disk. 
 * 
 * @author yanapanchenko
 */
public class ScanCrawlTask implements Runnable {
    
    private static final Logger logger = Logger.getLogger(ScanCrawlTask.class.getName());
    
    private final ScanCrawler scanCrawler;
	private ScanCacheFile scanCacheFiled;
	private AtomicReference<ScanCache> scanCacheAtom;

	public ScanCrawlTask(ScanCrawler scanCrawler, ScanCacheFile scanCacheFiled, AtomicReference<ScanCache> scanCacheAtom) {
        this.scanCrawler = scanCrawler;
		this.scanCacheFiled = scanCacheFiled;
		this.scanCacheAtom = scanCacheAtom;
    }

    @Override
    public void run() {
        
        logger.info("STARTING CACHING CORPORA SCAN");
        SimpleInMemScanCache cacheNew = new SimpleInMemScanCache();
        scanCrawler.crawl(cacheNew);
        logger.info("New Cache, number of root corpora: " + cacheNew.getRootCorpora().size());
        
        synchronized (scanCrawler) {
            if (cacheNew.isEmpty()) {
                logger.log(Level.INFO, "New cache is empty, no cache update performed");
            } else {
                logger.log(Level.INFO, "Started cache write into the file");
				scanCacheFiled.write(cacheNew);
				scanCacheAtom.set(cacheNew);
                logger.log(Level.INFO, "Finished cache write into the file");
            }
        }

        logger.info("FINISHED CACHING CORPORA SCAN");
    }
    
}
