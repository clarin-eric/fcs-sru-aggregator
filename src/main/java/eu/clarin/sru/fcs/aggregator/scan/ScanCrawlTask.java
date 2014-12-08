package eu.clarin.sru.fcs.aggregator.scan;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.clarin.sru.fcs.aggregator.client.ThrottledClient;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.LoggerFactory;

/**
 * @author yanapanchenko
 * @author edima
 */
public class ScanCrawlTask implements Runnable {

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(ScanCrawlTask.class);

	private ThrottledClient sruClient;
	private int cacheMaxDepth;
	private EndpointFilter filter;
	private AtomicReference<Corpora> corporaAtom;
	private File cachedCorpora;
	private AtomicReference<Statistics> statisticsAtom;
	private String centerRegistryUrl;

	public ScanCrawlTask(ThrottledClient sruClient, String centerRegistryUrl,
			int cacheMaxDepth, EndpointFilter filter,
			AtomicReference<Corpora> corporaAtom, File cachedCorpora,
			AtomicReference<Statistics> statisticsAtom
	) {
		this.sruClient = sruClient;
		this.centerRegistryUrl = centerRegistryUrl;
		this.cacheMaxDepth = cacheMaxDepth;
		this.filter = filter;
		this.corporaAtom = corporaAtom;
		this.cachedCorpora = cachedCorpora;
		this.statisticsAtom = statisticsAtom;
	}

	@Override
	public void run() {
		try {
			long time0 = System.currentTimeMillis();

			log.info("ScanCrawlTask: Initiating crawl");
			CenterRegistry centerRegistry = new CenterRegistryLive(centerRegistryUrl, filter);
			ScanCrawler scanCrawler = new ScanCrawler(centerRegistry, sruClient, cacheMaxDepth);

			log.info("ScanCrawlTask: Starting crawl");
			Corpora corpora = scanCrawler.crawl();

			corporaAtom.set(corpora);
			statisticsAtom.set(scanCrawler.getStatistics());
			long time = System.currentTimeMillis() - time0;

			log.info("ScanCrawlTask: crawl done in {}s, number of root corpora: {}",
					time / 1000., corpora.getCorpora().size());

			if (corpora.getCorpora().isEmpty()) {
				log.warn("ScanCrawlTask: Skipped writing to disk (no corpora). Finished.");
			} else {
				ObjectMapper mapper = new ObjectMapper();
				mapper.writerWithDefaultPrettyPrinter().writeValue(cachedCorpora, corpora);
				log.info("ScanCrawlTask: wrote to disk, finished");
			}
		} catch (IOException xc) {
			log.error("!!! Scan Crawler task IO exception", xc);
		} catch (Throwable xc) {
			log.error("!!! Scan Crawler task throwable exception", xc);
			throw xc;
		}
	}
}
