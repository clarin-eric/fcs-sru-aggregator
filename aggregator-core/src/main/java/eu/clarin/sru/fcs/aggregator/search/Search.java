package eu.clarin.sru.fcs.aggregator.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

import eu.clarin.sru.client.SRUClientConstants;
import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUSearchRetrieveRequest;
import eu.clarin.sru.client.SRUSearchRetrieveResponse;
import eu.clarin.sru.client.SRUVersion;
import eu.clarin.sru.client.fcs.ClarinFCSEndpointDescription.ResourceInfo.AvailabilityRestriction;
import eu.clarin.sru.client.fcs.ClarinFCSRecordData;
import eu.clarin.sru.client.fcs.LegacyClarinFCSRecordData;
import eu.clarin.sru.fcs.aggregator.client.CancellableOperation;
import eu.clarin.sru.fcs.aggregator.client.ThrottledClient;
import eu.clarin.sru.fcs.aggregator.core.AggregatorConstants;
import eu.clarin.sru.fcs.aggregator.scan.Diagnostic;
import eu.clarin.sru.fcs.aggregator.scan.FCSProtocolVersion;
import eu.clarin.sru.fcs.aggregator.scan.Resource;
import eu.clarin.sru.fcs.aggregator.scan.Statistics;
import eu.clarin.sru.fcs.aggregator.util.SRUCQL;
import eu.clarin.sru.fcs.aggregator.util.UniqueId;

/**
 * A search operation done on a list of resources.
 *
 * @author Yana Panchenko
 * @author edima
 * @author ljo
 */
public class Search {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Search.class);
    private static final org.slf4j.Logger logstatsSearch = LoggerFactory.getLogger("fcsstats.search");
    private static final org.slf4j.Logger logstatsResult = LoggerFactory.getLogger("fcsstats.result");

    private static final Pattern QUOTE_PATTERN = Pattern.compile("[\\s<>=/\\(\\)]");

    private final ThrottledClient searchClient;
    private final Statistics statistics;

    private final SRUVersion version;
    private final String queryType;
    private final String query;
    private final String searchLanguage;

    private final String id = UniqueId.generateId();
    private final long createdAt = System.currentTimeMillis();

    private final List<Result> results = Collections.synchronizedList(new ArrayList<>());

    public Search(ThrottledClient searchClient,
            PerformLanguageDetectionCallback langDetectCallback,
            SRUVersion version,
            Statistics statistics, List<Resource> resources,
            String queryType, String searchString,
            String searchLanguage, int startRecord, int maxRecords,
            final String userid) {
        this.searchClient = searchClient;

        this.version = version;
        this.queryType = queryType;
        this.query = quoteIfQuotableExpression(searchString, queryType);
        this.searchLanguage = searchLanguage;
        this.statistics = statistics;

        if (startRecord < 1) {
            startRecord = 1;
        }

        logstatsSearch.trace("[{}] queryType='{}' query='{}' language='{}'",
                this.id, this.queryType, this.query, this.searchLanguage);
        for (Resource resource : resources) {
            SRUVersion versionForResource = computeVersion(version, queryType, resource);
            Result result = new Result(resource, langDetectCallback);
            logstatsSearch.trace(
                    "[{}] endpoint='{}' resource='{}' sruversion='{}' batch={}+{} queryType='{}' query='{}'",
                    this.id, resource.getEndpoint().getUrl(), resource.getHandle(), versionForResource,
                    1, maxRecords, queryType, query);
            executeSearch(result, versionForResource, queryType, query, startRecord, maxRecords, userid);
            results.add(result);
        }
    }

    public boolean searchForNextResults(String resourceId, int maxRecords, final String userid) {
        for (Result r : results) {
            if (r.getResource().getId().equals(resourceId)) {
                SRUVersion versionForResource = computeVersion(version, queryType, r.getResource());
                logstatsSearch.trace(
                        "[{}] endpoint='{}' resource='{}' sruversion='{}' batch={}+{} queryType='{}' query='{}'",
                        this.id, r.getResource().getEndpoint().getUrl(), r.getResource().getHandle(),
                        versionForResource, r.getNextRecordPosition(), maxRecords, queryType, query);
                executeSearch(r, versionForResource, queryType, query, r.getNextRecordPosition(), maxRecords, userid);
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private void executeSearch(final Result result, SRUVersion version, String queryType, String searchString,
            int startRecord, int maxRecords, final String userid) {
        final Resource resource = result.getResource();
        log.info("Executing search in '{}' version='{}' queryType='{}' query='{}' maxRecords='{}'",
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

        if (resource.hasAvailabilityRestriction()) {
            // if the resource has an availability restriction, then add details for
            // authentication, otherwise we will not set it, to let the library not send
            // more information than neccessary
            // TODO: or check endpoint caps and always send auth info?
            if (userid != null) {
                // only if 'userid' is set, do we have an authenticated user, otherwise
                // the user is 'anonymous' (unauthenticated) and we do not want to send auth
                // infos that might confuse an endpoint to assume a user is authenticated
                searchRequest.setSendAuthentication(true);

                if (AvailabilityRestriction.PERSONAL_IDENTIFIER.equals(resource.getAvailabilityRestriction())) {
                    // and we only set the 'userid' if 'PERSONAL_IDENTIFIER' is requested, otherwise
                    // a valid JWT authentication signals a valid authentication at the aggregator
                    searchRequest.setAuthenticationContext(AggregatorConstants.PARAM_AUTHINFO_USERID, userid);
                }
            }
        }

        statistics.initEndpoint(resource.getEndpointInstitution(), resource.getEndpoint(),
                searchClient.getMaxConcurrentRequests(true, resource.getEndpoint().getUrl()));
        result.setInProgress(true);

        try {
            statistics.incrementOperationsCount(resource.getEndpointInstitution(), resource.getEndpoint());
            final CancellableOperation<?, ?> operation = searchClient.searchRetrieve(searchRequest,
                    new ThrottledClient.SearchCallback() {
                        @Override
                        public void onSuccess(SRUSearchRetrieveResponse response, ThrottledClient.Stats stats) {
                            try {
                                statistics.addEndpointDatapoint(resource.getEndpointInstitution(),
                                        resource.getEndpoint(),
                                        stats.getQueueTime(), stats.getExecutionTime());
                                log.debug("searchRetrieve request url: {}", response.getRequest().getRequestedURI());
                                result.setRequestUrl(response.getRequest().getRequestedURI().toString());
                                logstatsResult.trace(
                                        "[{}] endpoint='{}' resource='{}' numberOfRecords={} nextRecord={}",
                                        id, result.getResource().getEndpoint().getUrl(),
                                        result.getResource().getHandle(),
                                        (response != null) ? response.getNumberOfRecords() : null,
                                        (response != null) ? response.getNextRecordPosition() : null);
                                result.addResponse(response);
                                List<Diagnostic> diagnostics = result.getDiagnostics();
                                if (diagnostics != null && !diagnostics.isEmpty()) {
                                    log.error("diagnostic for url: {}",
                                            response.getRequest().getRequestedURI().toString());
                                    for (Diagnostic diagnostic : diagnostics) {
                                        statistics.addEndpointDiagnostic(resource.getEndpointInstitution(),
                                                resource.getEndpoint(),
                                                diagnostic, response.getRequest().getRequestedURI().toString());
                                    }
                                }
                            } catch (Throwable xc) {
                                log.error("search.onSuccess exception:", xc);
                            } finally {
                                result.setDone();
                                statistics.decrementOperationsCount(resource.getEndpointInstitution(),
                                        resource.getEndpoint());
                            }
                        }

                        @Override
                        public void onError(SRUSearchRetrieveRequest srureq, SRUClientException xc,
                                ThrottledClient.Stats stats) {
                            try {
                                statistics.addEndpointDatapoint(resource.getEndpointInstitution(),
                                        resource.getEndpoint(),
                                        stats.getQueueTime(), stats.getExecutionTime());
                                statistics.addErrorDatapoint(resource.getEndpointInstitution(), resource.getEndpoint(),
                                        xc,
                                        srureq.getRequestedURI().toString());
                                result.setRequestUrl(srureq.getRequestedURI().toString());
                                result.setException(xc);
                                log.error("search.onError:", xc);
                            } catch (Throwable xxc) {
                                log.error("search.onError exception:", xxc);
                            } finally {
                                result.setDone();
                                statistics.decrementOperationsCount(resource.getEndpointInstitution(),
                                        resource.getEndpoint());
                            }
                        }

                        @Override
                        public void onCancelled(SRUSearchRetrieveRequest srureq, ThrottledClient.Stats stats) {
                            try {
                                // TODO: how to update statistics?
                                // NOTE: the requested URI is not even set if no request happened...
                                // result.setRequestUrl(srureq.getRequestedURI().toString());
                                log.debug("search.onCancelled: [{}] endpoint='{}' resource='{}'", id,
                                        result.getResource().getEndpoint().getUrl(),
                                        result.getResource().getHandle());
                            } catch (Throwable xc) {
                                log.error("search.onCancelled exception:", xc);
                            } finally {
                                result.setCancelled();
                                statistics.decrementOperationsCount(resource.getEndpointInstitution(),
                                        resource.getEndpoint());
                            }
                        }
                    });
            // store operation to allow cancellation
            result.setSearchOperation(operation);
        } catch (Throwable xc) {
            log.error("SearchRetrieve error for " + resource.getEndpoint().getUrl(), xc);
        }
    }

    public boolean stopSearch() {
        // TODO: do we want to do a check beforehand to see if anything is even left to
        // cancel?
        synchronized (results) {
            for (final Result result : results) {
                result.setCancelled();
            }
        }
        return true;
    }

    public void shutdown() {
        // nothing to do
    }

    // ----------------------------------------------------------------------

    public void await() throws InterruptedException {
        // this should basically await all results
        // if a result is finished, await() should immediately return
        // if interrupted, we re-throw
        for (final Result result : results) {
            result.await();
        }
    }

    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        // from: https://stackoverflow.com/a/18668479/9360161
        // start await-ing each result
        final CountDownLatch metaLatch = new CountDownLatch(results.size());
        for (final Result result : results) {
            new Runnable() {
                @Override
                public void run() {
                    try {
                        // if result finished (not a timeout)
                        if (result.await(timeout, unit)) {
                            // then decrement our search results counter
                            metaLatch.countDown();
                        }
                    } catch (InterruptedException e) {
                        // do nothing?
                    }
                }
            }.run();
        }
        // return true if all results have finished
        return metaLatch.await(timeout, unit);
    }

    // ----------------------------------------------------------------------

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

    public String getId() {
        return id;
    }

    public Statistics getStatistics() {
        return statistics;
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

    public String getSearchLanguage() {
        return searchLanguage;
    }

    // ----------------------------------------------------------------------
    // TODO: do we need to synchronize here?

    public int getNumberOfResources() {
        synchronized (results) {
            return results.size();
        }
    }

    public int getNumberOfResourcesInProgress() {
        int inProgress = 0;
        synchronized (results) {
            for (Result r : results) {
                if (r.getInProgress()) {
                    inProgress++;
                }
            }
        }
        return inProgress;
    }

    public int getNumberOfResourcesCancelled() {
        int cancelled = 0;
        synchronized (results) {
            for (Result r : results) {
                if (r.getCancelled()) {
                    cancelled++;
                }
            }
        }
        return cancelled;
    }

    public boolean isFinished() {
        return getNumberOfResourcesInProgress() == 0;
    }

    // ----------------------------------------------------------------------

    private static SRUVersion computeVersion(SRUVersion version, String queryType, Resource resource) {
        // if a specific SRU version was requested, use it
        if (version != null) {
            return version;
        }

        // use SRU 2.0 if query type is FCS/LexFCS (required)
        if ("fcs".equals(queryType)) {
            return SRUVersion.VERSION_2_0;
        } else if ("lex".equals(queryType)) {
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

    static String quoteIfQuotableExpression(final String queryString, final String queryType) {
        if ("cql".equals(queryType)) {
            Matcher matcher = QUOTE_PATTERN.matcher(queryString.trim());
            boolean quotableFound = matcher.find();
            if (quotableFound && '"' != queryString.charAt(0)) {
                return "\"" + queryString + "\"";
            }
        }
        return queryString;
    }

} // class Search
