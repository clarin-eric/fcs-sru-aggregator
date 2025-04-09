package eu.clarin.sru.fcs.aggregator.scan;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.client.Client;

import org.slf4j.LoggerFactory;

import eu.clarin.sru.fcs.aggregator.client.ThrottledClient;

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
    private EndpointFilter endpointFilter;
    private CentreFilter centreFilter;
    private String centerRegistryUrl;
    private List<EndpointConfig> additionalCQLEndpoints;
    private List<EndpointConfig> additionalFCSEndpoints;
    private ScanCrawlTaskCompletedCallback callback;

    public interface ScanCrawlTaskCompletedCallback {
        void onSuccess(Resources resources, Statistics statistics);

        void onError(Throwable xc);
    }

    public ScanCrawlTask(ThrottledClient sruClient, Client jerseyClient, String centerRegistryUrl,
            int cacheMaxDepth,
            List<EndpointConfig> additionalCQLEndpoints,
            List<EndpointConfig> additionalFCSEndpoints,
            EndpointFilter endpointFilter,
            ScanCrawlTaskCompletedCallback callback) {
        this(sruClient, jerseyClient, centerRegistryUrl, cacheMaxDepth, additionalCQLEndpoints, additionalFCSEndpoints,
                endpointFilter, null, callback);
    }

    public ScanCrawlTask(ThrottledClient sruClient, Client jerseyClient, String centerRegistryUrl,
            int cacheMaxDepth,
            List<EndpointConfig> additionalCQLEndpoints,
            List<EndpointConfig> additionalFCSEndpoints,
            EndpointFilter endpointFilter,
            CentreFilter centreFilter,
            ScanCrawlTaskCompletedCallback callback) {
        this.sruClient = sruClient;
        this.jerseyClient = jerseyClient;
        this.centerRegistryUrl = centerRegistryUrl;
        this.cacheMaxDepth = cacheMaxDepth;
        this.additionalCQLEndpoints = additionalCQLEndpoints;
        this.additionalFCSEndpoints = additionalFCSEndpoints;
        this.endpointFilter = endpointFilter;
        this.centreFilter = centreFilter;
        this.callback = callback;
    }

    public static List<Institution> retrieveInstitutions(Client jerseyClient, String centerRegistryUrl) {
        return retrieveInstitutions(jerseyClient, centerRegistryUrl, null, null, null, null);
    }

    public static List<Institution> retrieveInstitutions(Client jerseyClient, String centerRegistryUrl,
            List<EndpointConfig> additionalCQLEndpoints, List<EndpointConfig> additionalFCSEndpoints) {
        return retrieveInstitutions(jerseyClient, centerRegistryUrl, additionalCQLEndpoints, additionalFCSEndpoints,
                null, null);
    }

    public static List<Institution> retrieveInstitutions(Client jerseyClient, String centerRegistryUrl,
            List<EndpointConfig> additionalCQLEndpoints, List<EndpointConfig> additionalFCSEndpoints,
            EndpointFilter endpointFilter) {
        return retrieveInstitutions(jerseyClient, centerRegistryUrl, additionalCQLEndpoints, additionalFCSEndpoints,
                endpointFilter, null);
    }

    public static List<Institution> retrieveInstitutions(Client jerseyClient, String centerRegistryUrl,
            List<EndpointConfig> additionalCQLEndpoints, List<EndpointConfig> additionalFCSEndpoints,
            EndpointFilter endpointFilter, CentreFilter centreFilter) {
        List<Institution> institutions = new ArrayList<>();

        // Query endpoints from centre registry
        if (centerRegistryUrl != null && !centerRegistryUrl.isEmpty()) {
            institutions = new CenterRegistryLive(centerRegistryUrl, endpointFilter, centreFilter, jerseyClient)
                    .getCQLInstitutions();
        }

        // Add sideloaded endpoints
        if (additionalCQLEndpoints != null && !additionalCQLEndpoints.isEmpty()) {
            // Add sideloaded endpoints with own name
            for (final EndpointConfig ep : additionalCQLEndpoints) {
                if (ep.getName() == null || ep.getName().isEmpty()) {
                    continue;
                }
                institutions.add(0, new Institution(ep.getName() + ", legacy", ep.getWebsite(), true) {
                    {
                        addEndpoint(ep.getUrl().toExternalForm(), FCSProtocolVersion.LEGACY);
                    }
                });
            }
            // Add sideloaded endpoints that have no name
            institutions.add(0,
                    new Institution("Unknown Institution, legacy", null, true) {
                        {
                            for (final EndpointConfig ep : additionalCQLEndpoints) {
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
            for (final EndpointConfig ep : additionalFCSEndpoints) {
                if (ep.getName() == null || ep.getName().isEmpty()) {
                    continue;
                }
                institutions.add(0, new Institution(ep.getName(), ep.getWebsite(), true) {
                    {
                        addEndpoint(ep.getUrl().toExternalForm(), FCSProtocolVersion.VERSION_2);
                    }
                });
            }
            // Add sideloaded endpoints that have no name
            institutions.add(0,
                    new Institution("Unknown Institution, FCS v2.0", null, true) {
                        {
                            for (final EndpointConfig ep : additionalFCSEndpoints) {
                                if (ep.getName() != null && !ep.getName().isEmpty()) {
                                    continue;
                                }
                                addEndpoint(ep.getUrl().toExternalForm(), FCSProtocolVersion.VERSION_2);
                            }
                        }
                    });
        }

        return institutions;
    }

    private List<Institution> retrieveInstitutions() {
        return retrieveInstitutions(jerseyClient, centerRegistryUrl, additionalCQLEndpoints, additionalFCSEndpoints,
                endpointFilter, centreFilter);
    }

    @Override
    public void run() {
        try {
            long time0 = System.currentTimeMillis();

            log.info("ScanCrawlTask: Initiating crawl");
            List<Institution> institutions = retrieveInstitutions();

            ScanCrawler scanCrawler = new ScanCrawler(institutions, sruClient, cacheMaxDepth);

            log.info("ScanCrawlTask: Starting crawl");
            Resources resources = scanCrawler.crawl();

            long time = System.currentTimeMillis() - time0;
            log.info("ScanCrawlTask: crawl done in {}s, number of root resources: {}",
                    time / 1000., resources.getResources().size());

            callback.onSuccess(resources, scanCrawler.getStatistics());
        } catch (Throwable xc) {
            log.error("ScanCrawlTask: throwable exception", xc);
            callback.onError(xc);
            throw xc;
        }
    }

}
