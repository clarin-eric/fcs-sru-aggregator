package eu.clarin.sru.fcs.aggregator.scan;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.clarin.sru.fcs.aggregator.client.ThrottledClient;
import io.dropwizard.setup.Environment;
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
 *
 * This task is run by an executor every now and then to scan for new endpoints
 */
public class ScanCrawlTask implements Runnable {

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(ScanCrawlTask.class);

	private ThrottledClient sruClient;
	private int cacheMaxDepth;
	private EndpointFilter filter;
	private AtomicReference<Corpora> corporaAtom;
	private File cachedCorpora;
	private File oldCachedCorpora;
	private AtomicReference<Statistics> scanStatisticsAtom;
	private AtomicReference<Statistics> searchStatisticsAtom;
	private String centerRegistryUrl;
	private List<URL> additionalCQLEndpoints;
	private List<URL> additionalFCSEndpoints;

        private final Environment environment;
	public ScanCrawlTask(ThrottledClient sruClient, String centerRegistryUrl,
			int cacheMaxDepth,
			List<URL> additionalCQLEndpoints,
			List<URL> additionalFCSEndpoints,
			EndpointFilter filter,
			AtomicReference<Corpora> corporaAtom,
			File cachedCorpora, File oldCachedCorpora,
			AtomicReference<Statistics> scanStatisticsAtom,
			AtomicReference<Statistics> searchStatisticsAtom,
                        Environment environment
	) {
		this.sruClient = sruClient;
		this.centerRegistryUrl = centerRegistryUrl;
		this.cacheMaxDepth = cacheMaxDepth;
		this.additionalCQLEndpoints = additionalCQLEndpoints;
		this.additionalFCSEndpoints = additionalFCSEndpoints;
		this.filter = filter;
		this.corporaAtom = corporaAtom;
		this.cachedCorpora = cachedCorpora;
		this.oldCachedCorpora = oldCachedCorpora;
		this.scanStatisticsAtom = scanStatisticsAtom;
		this.searchStatisticsAtom = searchStatisticsAtom;
                this.environment = environment;
	}

	@Override
	public void run() {
		try {
			long time0 = System.currentTimeMillis();

			log.info("ScanCrawlTask: Initiating crawl");
			List<Institution> institutions = new ArrayList<Institution>();
			if (centerRegistryUrl != null && !centerRegistryUrl.isEmpty()) {
				institutions = new CenterRegistryLive(centerRegistryUrl, filter, environment).getCQLInstitutions();
			}
			if (additionalCQLEndpoints != null && !additionalCQLEndpoints.isEmpty()) {
				institutions.add(0,
						new Institution("Unknown Institution, legacy", null) {
							{
								for (URL u : additionalCQLEndpoints) {
									addEndpoint(u.toExternalForm(), FCSProtocolVersion.LEGACY);
								}
							}
						});
			}
			if (additionalFCSEndpoints != null && !additionalFCSEndpoints.isEmpty()) {
				institutions.add(0,
						new Institution("Unknown Institution, FCS v2.0", null) {
							{
								for (URL u : additionalFCSEndpoints) {
									addEndpoint(u.toExternalForm(), FCSProtocolVersion.VERSION_2);
								}
							}
						});
			}
			ScanCrawler scanCrawler = new ScanCrawler(institutions, sruClient, cacheMaxDepth);

			log.info("ScanCrawlTask: Starting crawl");
			Corpora corpora = scanCrawler.crawl();

			long time = System.currentTimeMillis() - time0;
			log.info("ScanCrawlTask: crawl done in {}s, number of root corpora: {}",
					time / 1000., corpora.getCorpora().size());

			if (corpora.getCorpora().isEmpty()) {
				log.warn("ScanCrawlTask: No corpora: skipped updating stats; skipped writing to disk.");
			} else {
				corporaAtom.set(corpora);
				scanStatisticsAtom.set(scanCrawler.getStatistics());
				searchStatisticsAtom.set(new Statistics()); // reset search stats

				dump(corpora, cachedCorpora, oldCachedCorpora);
				log.info("ScanCrawlTask: wrote to disk, finished");
			}
		} catch (IOException xc) {
			log.error("!!! Scan Crawler task IO exception", xc);
		} catch (Throwable xc) {
			log.error("!!! Scan Crawler task throwable exception", xc);
			throw xc;
		}
	}

	private static void dump(Corpora corpora,
			File cachedCorpora, File oldCachedCorpora) throws IOException {
		if (cachedCorpora.exists()) {
			try {
				oldCachedCorpora.delete();
			} catch (Throwable txc) {
				//ignore
			}
			try {
				cachedCorpora.renameTo(oldCachedCorpora);
			} catch (Throwable txc) {
				// ignore
			}
		}
		ObjectMapper mapper = new ObjectMapper();
		mapper.writerWithDefaultPrettyPrinter().writeValue(cachedCorpora, corpora);
	}
}
