package eu.clarin.sru.fcs.aggregator.cache;

import eu.clarin.sru.fcs.aggregator.cache.ScanCrawler;
import eu.clarin.sru.fcs.aggregator.cache.ScanCacheFiled;
import eu.clarin.sru.fcs.aggregator.cache.ScanCache;
import static eu.clarin.sru.fcs.aggregator.app.WebAppListener.CORPUS_CACHE;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.zkoss.zk.ui.WebApp;

/**
 *
 * @author yanapanchenko
 */
public class ScanCrawlerRunnable implements Runnable {
    
    private static final Logger logger = Logger.getLogger(ScanCrawlerRunnable.class.getName());
    
    private final ScanCrawler scanCrawler;
    private ScanCacheFiled scanCacheFiled;
    private WebApp webapp;

    public ScanCrawlerRunnable(
            ScanCrawler scanCrawler, ScanCacheFiled scanCacheFiled, WebApp webapp) {
        this.scanCrawler = scanCrawler;
        this.scanCacheFiled = scanCacheFiled;
        this.webapp = webapp;
    }

    @Override
    public void run() {
        
        logger.info("STARTING CACHING CORPORA SCAN");
        ScanCache cacheNew = scanCrawler.crawl();
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
