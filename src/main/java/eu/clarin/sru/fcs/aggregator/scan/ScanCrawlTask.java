package eu.clarin.sru.fcs.aggregator.scan;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.clarin.sru.fcs.aggregator.client.ThrottledClient;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
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
	private AtomicReference<Statistics> scanStatisticsAtom;
	private AtomicReference<Statistics> searchStatisticsAtom;
	private String centerRegistryUrl;
	private List<URL> additionalCQLEndpoints;

	public ScanCrawlTask(ThrottledClient sruClient, String centerRegistryUrl,
			int cacheMaxDepth, List<URL> additionalCQLEndpoints,
			EndpointFilter filter,
			AtomicReference<Corpora> corporaAtom, File cachedCorpora,
			AtomicReference<Statistics> scanStatisticsAtom,
			AtomicReference<Statistics> searchStatisticsAtom
			) {
		this.sruClient = sruClient;
		this.centerRegistryUrl = centerRegistryUrl;
		this.cacheMaxDepth = cacheMaxDepth;
		this.additionalCQLEndpoints = additionalCQLEndpoints;
		this.filter = filter;
		this.corporaAtom = corporaAtom;
		this.cachedCorpora = cachedCorpora;
		this.scanStatisticsAtom = scanStatisticsAtom;
		this.searchStatisticsAtom = searchStatisticsAtom;
	}

	@Override
	public void run() {
		try {
			long time0 = System.currentTimeMillis();

			log.info("ScanCrawlTask: Initiating crawl");
			List<Institution> institutions = new ArrayList<Institution>();
			if (centerRegistryUrl != null && !centerRegistryUrl.isEmpty()) {
				institutions = new CenterRegistryLive(centerRegistryUrl, filter).getCQLInstitutions();
			}
			if (additionalCQLEndpoints != null && !additionalCQLEndpoints.isEmpty()) {
				institutions.add(0,
						new Institution("ZZZ", null) {
							{
								for (URL u : additionalCQLEndpoints) {
									addEndpoint(u.toExternalForm());
								}
							}
						});
			}
			ScanCrawler scanCrawler = new ScanCrawler(institutions, sruClient, cacheMaxDepth);

			log.info("ScanCrawlTask: Starting crawl");
			Corpora corpora = scanCrawler.crawl();

			corporaAtom.set(corpora);
			scanStatisticsAtom.set(scanCrawler.getStatistics());
			searchStatisticsAtom.set(new Statistics()); // reset search stats
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
