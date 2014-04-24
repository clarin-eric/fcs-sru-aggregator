package eu.clarin.sru.fcs.aggregator.cache;

import static eu.clarin.sru.fcs.aggregator.app.WebAppListener.CORPUS_CACHE;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.zkoss.zk.ui.WebApp;

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
    private ScanCacheFiled scanCacheFiled;
    private WebApp webapp;

    public ScanCrawlTask(
            ScanCrawler scanCrawler, ScanCacheFiled scanCacheFiled, WebApp webapp) {
        this.scanCrawler = scanCrawler;
        this.scanCacheFiled = scanCacheFiled;
        this.webapp = webapp;
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
                webapp.setAttribute(CORPUS_CACHE, cacheNew);
                logger.log(Level.INFO, "Finished cache write into the file");
            }
        }

        logger.info("FINISHED CACHING CORPORA SCAN");
    }
    
}
