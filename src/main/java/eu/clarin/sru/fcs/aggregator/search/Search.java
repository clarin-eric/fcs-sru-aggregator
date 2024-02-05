package eu.clarin.sru.fcs.aggregator.search;

import java.util.List;
import eu.clarin.sru.client.SRUClientConstants;
import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUSearchRetrieveRequest;
import eu.clarin.sru.client.SRUSearchRetrieveResponse;
import eu.clarin.sru.client.SRUVersion;
import eu.clarin.sru.client.fcs.ClarinFCSRecordData;
import eu.clarin.sru.client.fcs.LegacyClarinFCSRecordData;
import eu.clarin.sru.fcs.aggregator.client.ThrottledClient;
import eu.clarin.sru.fcs.aggregator.scan.Resource;
import eu.clarin.sru.fcs.aggregator.scan.Diagnostic;
import eu.clarin.sru.fcs.aggregator.scan.FCSProtocolVersion;
import eu.clarin.sru.fcs.aggregator.scan.Statistics;
import eu.clarin.sru.fcs.aggregator.util.SRUCQL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.LoggerFactory;

/**
 * A search operation done on a list of resources.
 *
 * @author Yana Panchenko
 * @author edima
 * @author ljo
 */
public class Search {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Search.class);

    static final int EXPORTS_SIZE_GC_THRESHOLD = 3;

    private static final AtomicLong counter = new AtomicLong(Math.abs(new Random().nextInt()));

    private final ThrottledClient searchClient;
    private final SRUVersion version;
    private final Long id;
    private final String queryType;
    private final String query;
    private final long createdAt = System.currentTimeMillis();
    private final String searchLanguage;
    private final List<Result> results = Collections.synchronizedList(new ArrayList<Result>());
    private final Statistics statistics;
    private static final Pattern quotePattern = Pattern.compile("[\\s<>=/\\(\\)]");

    public Search(ThrottledClient searchClient,
            SRUVersion version,
            Statistics statistics, List<Resource> resources,
            String queryType, String searchString,
            String searchLanguage, int maxRecords) {
        this.searchClient = searchClient;
        this.version = version;
        this.id = counter.getAndIncrement();
        this.queryType = queryType;
        this.query = quoteIfQuotableExpression(searchString, queryType);
        this.searchLanguage = searchLanguage;
        this.statistics = statistics;
        for (Resource resource : resources) {
            Result result = new Result(resource);
            SRUVersion versionForResource = computeVersion(this.version, queryType, resource);
            executeSearch(result, versionForResource, queryType, query, 1, maxRecords);
            results.add(result);
        }
    }

    private SRUVersion computeVersion(SRUVersion version, String queryType, Resource resource) {
        // if a specific SRU version was requested, use it
        if (version != null) {
            return version;
        }
        // use SRU 2.0 if query type is FCS (required)
        if ("fcs".equals(queryType)) {
            return SRUVersion.VERSION_2_0;
        }
        // otherwise go by resource->endpoint version (version of endpoint description,
        // not SRU Server Config!)
        if (resource.getEndpoint().getProtocol() == FCSProtocolVersion.VERSION_2) {
            return SRUVersion.VERSION_2_0;
        } else if (resource.getEndpoint().getProtocol() == FCSProtocolVersion.VERSION_1) {
            return SRUVersion.VERSION_1_2;
        }
        // TODO: what to do with FCSProtocolVersion.LEGACY ? --> SRU 1.1 / SRU 1.2
        // default to 1.2
        return SRUVersion.VERSION_1_2;
    }

    public boolean searchForNextResults(String resourceId, int maxRecords) {
        for (Result r : results) {
            if (r.getResource().getId().equals(resourceId)) {
                SRUVersion versionForResource = computeVersion(version, queryType, r.getResource());
                executeSearch(r, versionForResource, queryType, query, r.getNextRecordPosition(), maxRecords);
                return true;
            }
        }
        return false;
    }

    private void executeSearch(final Result result, SRUVersion version, String queryType, String searchString,
            int startRecord, int maxRecords) {
        final Resource resource = result.getResource();
        log.info("Executing search in '{}' version='{}' queryType ='{}' query='{}' maxRecords='{}'",
                resource, version, queryType, searchString, maxRecords);

        SRUSearchRetrieveRequest searchRequest = new SRUSearchRetrieveRequest(resource.getEndpoint().getUrl());
        searchRequest.setVersion(version);
        searchRequest.setStartRecord(startRecord);
        searchRequest.setMaximumRecords(maxRecords);
        boolean legacy = resource.getEndpoint().getProtocol().equals(FCSProtocolVersion.LEGACY);
        searchRequest.setRecordSchema(legacy
                ? LegacyClarinFCSRecordData.RECORD_SCHEMA
                : ClarinFCSRecordData.RECORD_SCHEMA);
        searchRequest
                .setQuery((legacy || version.compareTo(SRUVersion.VERSION_2_0) < 0) ? SRUClientConstants.QUERY_TYPE_CQL
                        : queryType, searchString);
        if (resource.getHandle() != null) {
            searchRequest.setExtraRequestData(legacy
                    ? SRUCQL.SEARCH_RESOURCE_HANDLE_LEGACY_PARAMETER
                    : SRUCQL.SEARCH_RESOURCE_HANDLE_PARAMETER,
                    resource.getHandle());
        }

        statistics.initEndpoint(resource.getEndpointInstitution(), resource.getEndpoint(),
                searchClient.getMaxConcurrentRequests(true, resource.getEndpoint().getUrl()));
        result.setInProgress(true);

        try {
            searchClient.searchRetrieve(searchRequest, new ThrottledClient.SearchCallback() {
                @Override
                public void onSuccess(SRUSearchRetrieveResponse response, ThrottledClient.Stats stats) {
                    try {
                        statistics.addEndpointDatapoint(resource.getEndpointInstitution(), resource.getEndpoint(),
                                stats.getQueueTime(), stats.getExecutionTime());
                        result.addResponse(response);
                        List<Diagnostic> diagnostics = result.getDiagnostics();
                        if (diagnostics != null && !diagnostics.isEmpty()) {
                            log.error("diagnostic for url: {}", response.getRequest().getRequestedURI().toString());
                            for (Diagnostic diagnostic : diagnostics) {
                                statistics.addEndpointDiagnostic(resource.getEndpointInstitution(), resource.getEndpoint(),
                                        diagnostic, response.getRequest().getRequestedURI().toString());
                            }
                        }
                    } catch (Throwable xc) {
                        log.error("search.onSuccess exception:", xc);
                    } finally {
                        result.setInProgress(false);
                    }
                }

                @Override
                public void onError(SRUSearchRetrieveRequest srureq, SRUClientException xc,
                        ThrottledClient.Stats stats) {
                    try {
                        statistics.addEndpointDatapoint(resource.getEndpointInstitution(), resource.getEndpoint(),
                                stats.getQueueTime(), stats.getExecutionTime());
                        statistics.addErrorDatapoint(resource.getEndpointInstitution(), resource.getEndpoint(), xc,
                                srureq.getRequestedURI().toString());
                        result.setException(xc);
                        log.error("search.onError:", xc);
                    } catch (Throwable xxc) {
                        log.error("search.onError exception:", xxc);
                    } finally {
                        result.setInProgress(false);
                    }
                }
            });
        } catch (Throwable xc) {
            log.error("SearchRetrieve error for " + resource.getEndpoint().getUrl(), xc);
        }
    }

    public Long getId() {
        return id;
    }

    public List<Result> getResults(String resourceId) {
        List<Result> copy = new ArrayList<>();
        synchronized (results) {
            if (resourceId == null || resourceId.isEmpty()) {
                copy.addAll(results);
            } else {
                for (Result r : results) {
                    if (resourceId.equals(r.getResource().getId())) {
                        copy.add(r);
                    }
                }
            }
        }
        return copy;
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public void shutdown() {
        // nothing to do
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getQueryType() {
        return queryType;
    }

    public String getQuery() {
        return query;
    }

    protected static String quoteIfQuotableExpression(final String queryString, final String queryType) {
        Matcher matcher = quotePattern.matcher(queryString.trim());
        boolean quotableFound = matcher.find();
        if ("cql".equals(queryType) && quotableFound && !"\"".equals(queryString.charAt(0))) {
            return "\"" + queryString + "\"";
        }
        return queryString;
    }

    public String getSearchLanguage() {
        return searchLanguage;
    }

    // ----------------------------------------------------------------------

    public static class WeblichtExportCacheEntry {
        private static final AtomicLong counter = new AtomicLong(Math.abs(new Random().nextInt()));

        private final Long id;
        private final byte[] data;

        public WeblichtExportCacheEntry(byte[] data) {
            this.id = counter.getAndIncrement();
            this.data = data;
        }

        public Long getId() {
            return id;
        }

        public byte[] getData() {
            return data;
        }
    }

    private final List<WeblichtExportCacheEntry> exports = Collections
            .synchronizedList(new ArrayList<WeblichtExportCacheEntry>());

    public byte[] getWeblichtExport(Long exportId) {
        synchronized (exports) {
            for (WeblichtExportCacheEntry export : exports) {
                if (exportId.equals(export.getId())) {
                    return export.getData();
                }
            }
        }
        return null;
    }

    public Long addWeblichtExport(byte[] data) {
        synchronized (exports) {
            // check if data already exists in cache
            int dataHash = Arrays.hashCode(data);
            for (WeblichtExportCacheEntry export : exports) {
                if (Arrays.hashCode(export.data) == dataHash && Arrays.equals(data, export.data)) {
                    return export.getId();
                }
            }
            // needs to add new entry but check first if we need to evict old ones
            if (exports.size() > EXPORTS_SIZE_GC_THRESHOLD) {
                exports.remove(0);
            }
            // create and add new entry
            WeblichtExportCacheEntry export = new WeblichtExportCacheEntry(data);
            exports.add(export);
            return export.getId();
        }
    }

}
