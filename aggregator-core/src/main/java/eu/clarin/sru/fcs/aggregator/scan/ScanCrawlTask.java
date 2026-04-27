package eu.clarin.sru.fcs.aggregator.scan;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.LoggerFactory;

import eu.clarin.sru.fcs.aggregator.client.ThrottledClient;
import eu.clarin.sru.fcs.aggregator.scan.centre_registry.CenterRegistry;

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
    private String centerRegistryUrl;
    private List<EndpointOverrideConfig> endpointOverrides;
    private ScanCrawlTaskCompletedCallback callback;

    public interface ScanCrawlTaskCompletedCallback {
        void onSuccess(Resources resources, Statistics statistics);

        void onError(Throwable xc);
    }

    public ScanCrawlTask(ThrottledClient sruClient, Client jerseyClient, String centerRegistryUrl, int cacheMaxDepth,
            List<EndpointOverrideConfig> endpointOverrides, EndpointFilter endpointFilter,
            ScanCrawlTaskCompletedCallback callback) {
        this.sruClient = sruClient;
        this.jerseyClient = jerseyClient;
        this.centerRegistryUrl = centerRegistryUrl;
        this.cacheMaxDepth = cacheMaxDepth;
        this.endpointOverrides = endpointOverrides;
        this.endpointFilter = endpointFilter;
        this.callback = callback;
    }

    public static List<Institution> retrieveInstitutions(Client jerseyClient, String centerRegistryUrl) {
        return retrieveInstitutions(jerseyClient, centerRegistryUrl, null, null);
    }

    public static List<Institution> retrieveInstitutions(Client jerseyClient, String centerRegistryUrl,
            List<EndpointOverrideConfig> endpointOverrides, EndpointFilter endpointFilter) {
        List<Institution> institutions = null;

        // Query endpoints from centre registry
        if (centerRegistryUrl != null && !centerRegistryUrl.isEmpty()) {
            institutions = new CenterRegistry(jerseyClient, centerRegistryUrl, endpointFilter)
                    .retrieveInstitutionsWithFCSEndpoints();
        }

        if (institutions == null) {
            institutions = new ArrayList<>();
        }

        if (endpointOverrides != null && !endpointOverrides.isEmpty()) {
            // Add sideloaded endpoints
            // (that are enabled and not only configuration overrides)
            final Map<?, List<EndpointOverrideConfig>> groupedEndpoints = endpointOverrides.stream()
                    .filter(EndpointOverrideConfig::isEnabled)
                    .filter(Predicate.not(EndpointOverrideConfig::isOverrideOnly))
                    .collect(Collectors.groupingBy(ep -> new ImmutablePair<>(ep.getName(), ep.getWebsite())));
            for (final List<EndpointOverrideConfig> endpoints : groupedEndpoints.values()) {
                EndpointOverrideConfig firstEndpoint = endpoints.get(0);

                final String name = (firstEndpoint.getName() != null && !firstEndpoint.getName().isEmpty())
                        ? firstEndpoint.getName()
                        : "Unknown Institution";
                final String website = (firstEndpoint.getWebsite() != null && !firstEndpoint.getWebsite().isEmpty())
                        ? firstEndpoint.getWebsite()
                        : null;
                final Institution institution = new Institution(name, website, true);

                for (final EndpointOverrideConfig endpoint : endpoints) {
                    final FCSProtocolVersion version = endpoint.isCQL()
                            ? FCSProtocolVersion.LEGACY
                            : FCSProtocolVersion.VERSION_2;
                    institution.addEndpoint(endpoint.getUrl().toExternalForm(), version);
                }

                log.info("Add institution '{}' with {} endpoints", institution, institution.getEndpoints().size());
                institutions.add(0, institution);
            }

            // TODO: do we want to check and (metadata) override existing endpoints?

            // now filter...
            final List<URL> disabledEndpoints = endpointOverrides.stream()
                    .filter(o -> !o.isEnabled())
                    .map(EndpointOverrideConfig::getUrl).collect(Collectors.toList());
            for (int i = institutions.size() - 1; i >= 0; i--) {
                final Institution institution = institutions.get(i);

                // remove endpoints
                final Set<Endpoint> endpoints2remove = new HashSet<>();
                for (final Endpoint endpoint : institution.getEndpoints()) {
                    URL endpointUrl = null;
                    try {
                        endpointUrl = new URL(endpoint.getUrl());
                    } catch (MalformedURLException e) {
                        log.warn("Somehow got an URL error for '{}': {}", endpoint.getUrl(), e.getLocalizedMessage());
                    }
                    if (endpointUrl != null && disabledEndpoints.contains(endpointUrl)) {
                        endpoints2remove.add(endpoint);
                        log.info("Remove endpoint: {}", endpoint);
                    }
                }
                institution.getEndpoints().removeAll(endpoints2remove);

                // then remove institutions without endpoints left
                if (institution.getEndpoints().isEmpty()) {
                    log.info("Remove institution: {}", institution);
                    institutions.remove(i);
                }
            }
        }

        return institutions;
    }

    private List<Institution> retrieveInstitutions() {
        return retrieveInstitutions(jerseyClient, centerRegistryUrl, endpointOverrides, endpointFilter);
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
