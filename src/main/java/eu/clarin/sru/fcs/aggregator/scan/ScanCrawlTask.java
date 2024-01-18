package eu.clarin.sru.fcs.aggregator.scan;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.clarin.sru.fcs.aggregator.app.AggregatorConfiguration;
import eu.clarin.sru.fcs.aggregator.client.ThrottledClient;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.client.Client;

import org.slf4j.LoggerFactory;

/**
 * This task is run by an executor every now and then to scan for new endpoints.
 * 
 * @author yanapanchenko
 * @author edima
 */
public class ScanCrawlTask implements Runnable {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ScanCrawlTask.class);

    private final ThrottledClient sruClient;
    private final Client jerseyClient;
    private int cacheMaxDepth;
    private EndpointFilter filter;
    private AtomicReference<Resources> resourcesAtom;
    private File cachedResources;
    private File oldCachedResources;
    private AtomicReference<Statistics> scanStatisticsAtom;
    private AtomicReference<Statistics> searchStatisticsAtom;
    private String centerRegistryUrl;
    private List<AggregatorConfiguration.Params.EndpointConfig> additionalCQLEndpoints;
    private List<AggregatorConfiguration.Params.EndpointConfig> additionalFCSEndpoints;

    public ScanCrawlTask(ThrottledClient sruClient, Client jerseyClient, String centerRegistryUrl,
            int cacheMaxDepth,
            List<AggregatorConfiguration.Params.EndpointConfig> additionalCQLEndpoints,
            List<AggregatorConfiguration.Params.EndpointConfig> additionalFCSEndpoints,
            EndpointFilter filter,
            AtomicReference<Resources> resourcesAtom,
            File cachedResources, File oldCachedResources,
            AtomicReference<Statistics> scanStatisticsAtom,
            AtomicReference<Statistics> searchStatisticsAtom) {
        this.sruClient = sruClient;
        this.jerseyClient = jerseyClient;
        this.centerRegistryUrl = centerRegistryUrl;
        this.cacheMaxDepth = cacheMaxDepth;
        this.additionalCQLEndpoints = additionalCQLEndpoints;
        this.additionalFCSEndpoints = additionalFCSEndpoints;
        this.filter = filter;
        this.resourcesAtom = resourcesAtom;
        this.cachedResources = cachedResources;
        this.oldCachedResources = oldCachedResources;
        this.scanStatisticsAtom = scanStatisticsAtom;
        this.searchStatisticsAtom = searchStatisticsAtom;
    }

    @Override
    public void run() {
        try {
            long time0 = System.currentTimeMillis();

            log.info("ScanCrawlTask: Initiating crawl");
            List<Institution> institutions = new ArrayList<Institution>();
            // Query endpoints from centre registry
            if (centerRegistryUrl != null && !centerRegistryUrl.isEmpty()) {
                institutions = new CenterRegistryLive(centerRegistryUrl, filter, jerseyClient).getCQLInstitutions();
            }
            // Add sideloaded endpoints
            if (additionalCQLEndpoints != null && !additionalCQLEndpoints.isEmpty()) {
                // Add sideloaded endpoints with own name
                for (final AggregatorConfiguration.Params.EndpointConfig ep : additionalCQLEndpoints) {
                    if (ep.getName() == null || ep.getName().isEmpty()) {
                        continue;
                    }
                    institutions.add(0, new Institution(ep.getName() + ", legacy", ep.getWebsite()) {
                        {
                            addEndpoint(ep.getUrl().toExternalForm(), FCSProtocolVersion.LEGACY);
                        }
                    });
                }
                // Add sideloaded endpoints that have no name
                institutions.add(0,
                        new Institution("Unknown Institution, legacy", null) {
                            {
                                for (final AggregatorConfiguration.Params.EndpointConfig ep : additionalCQLEndpoints) {
                                    if (ep.getName() != null && !ep.getName().isEmpty()) {
                                        continue;
                                    }
                                    addEndpoint(ep.getUrl().toExternalForm(), FCSProtocolVersion.LEGACY);
                                }
                            }
                        });
            }
            if (additionalFCSEndpoints != null && !additionalFCSEndpoints.isEmpty()) {
                // Add sideloaded endpoints with own name
                for (final AggregatorConfiguration.Params.EndpointConfig ep : additionalFCSEndpoints) {
                    if (ep.getName() == null || ep.getName().isEmpty()) {
                        continue;
                    }
                    institutions.add(0, new Institution(ep.getName() + ", FCS v2.0", ep.getWebsite()) {
                        {
                            addEndpoint(ep.getUrl().toExternalForm(), FCSProtocolVersion.VERSION_2);
                        }
                    });
                }
                // Add sideloaded endpoints that have no name
                institutions.add(0,
                        new Institution("Unknown Institution, FCS v2.0", null) {
                            {
                                for (final AggregatorConfiguration.Params.EndpointConfig ep : additionalFCSEndpoints) {
                                    if (ep.getName() != null && !ep.getName().isEmpty()) {
                                        continue;
                                    }
                                    addEndpoint(ep.getUrl().toExternalForm(), FCSProtocolVersion.VERSION_2);
                                }
                            }
                        });
            }

            ScanCrawler scanCrawler = new ScanCrawler(institutions, sruClient, cacheMaxDepth);

            log.info("ScanCrawlTask: Starting crawl");
            Resources resources = scanCrawler.crawl();

            long time = System.currentTimeMillis() - time0;
            log.info("ScanCrawlTask: crawl done in {}s, number of root resources: {}",
                    time / 1000., resources.getResources().size());

            if (resources.getResources().isEmpty()) {
                log.warn("ScanCrawlTask: No resources: skipped updating stats; skipped writing to disk.");
            } else {
                resourcesAtom.set(resources);
                scanStatisticsAtom.set(scanCrawler.getStatistics());
                searchStatisticsAtom.set(new Statistics()); // reset search stats

                dump(resources, cachedResources, oldCachedResources);
                log.info("ScanCrawlTask: wrote to disk, finished");
            }
        } catch (IOException xc) {
            log.error("!!! Scan Crawler task IO exception", xc);
        } catch (Throwable xc) {
            log.error("!!! Scan Crawler task throwable exception", xc);
            throw xc;
        }
    }

    private static void dump(Resources resources,
            File cachedResources, File oldCachedResources) throws IOException {
        if (cachedResources.exists()) {
            try {
                oldCachedResources.delete();
            } catch (Throwable txc) {
                // ignore
            }
            try {
                cachedResources.renameTo(oldCachedResources);
            } catch (Throwable txc) {
                // ignore
            }
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(cachedResources, resources);
    }
}
