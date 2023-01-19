package eu.clarin.sru.fcs.aggregator.search;

import java.util.List;
import eu.clarin.sru.client.SRUClientConstants;
import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUSearchRetrieveRequest;
import eu.clarin.sru.client.SRUSearchRetrieveResponse;
import eu.clarin.sru.client.SRUVersion;
import eu.clarin.sru.client.fcs.ClarinFCSRecordData;
import eu.clarin.sru.fcs.aggregator.client.ThrottledClient;
import eu.clarin.sru.fcs.aggregator.scan.Corpus;
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
 * A search operation done on a list of corpora.
 *
 * @author Yana Panchenko
 * @author edima
 * @author ljo
 */
public class Search {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Search.class);

    private static final String SEARCH_RESULTS_ENCODING = "UTF-8";
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
            Statistics statistics, List<Corpus> corpora,
            String queryType, String searchString,
            String searchLanguage, int maxRecords) {
        this.searchClient = searchClient;
        this.version = version;
        this.id = counter.getAndIncrement();
        this.queryType = queryType;
        this.query = quoteIfQuotableExpression(searchString, queryType);
        this.searchLanguage = searchLanguage;
        this.statistics = statistics;
        for (Corpus corpus : corpora) {
            Result result = new Result(corpus);
            executeSearch(result, version, queryType, query, 1, maxRecords);
            results.add(result);
        }
    }

    public boolean searchForNextResults(String corpusId, int maxRecords) {
        for (Result r : results) {
            if (r.getCorpus().getId().equals(corpusId)) {
                executeSearch(r, version, queryType, query, r.getNextRecordPosition(), maxRecords);
                return true;
            }
        }
        return false;
    }

    private void executeSearch(final Result result, SRUVersion version, String queryType, String searchString,
            int startRecord, int maxRecords) {
        final Corpus corpus = result.getCorpus();
        log.info("Executing search in '{}' version='{}' queryType ='{}' query='{}' maxRecords='{}'",
                corpus, version, queryType, searchString, maxRecords);

        SRUSearchRetrieveRequest searchRequest = new SRUSearchRetrieveRequest(corpus.getEndpoint().getUrl());
        searchRequest.setVersion(version);
        searchRequest.setStartRecord(startRecord);
        searchRequest.setMaximumRecords(maxRecords);
        boolean legacy = corpus.getEndpoint().getProtocol().equals(FCSProtocolVersion.LEGACY);
        searchRequest.setRecordSchema(legacy
                ? ClarinFCSRecordData.LEGACY_RECORD_SCHEMA
                : ClarinFCSRecordData.RECORD_SCHEMA);
        searchRequest.setQuery(
                (legacy || version != SRUVersion.VERSION_2_0) ? SRUClientConstants.QUERY_TYPE_CQL : "fcs",
                searchString);
        if (corpus.getHandle() != null) {
            searchRequest.setExtraRequestData(legacy
                    ? SRUCQL.SEARCH_CORPUS_HANDLE_LEGACY_PARAMETER
                    : SRUCQL.SEARCH_CORPUS_HANDLE_PARAMETER,
                    corpus.getHandle());
        }

        statistics.initEndpoint(corpus.getInstitution(), corpus.getEndpoint(),
                searchClient.getMaxConcurrentRequests(true, corpus.getEndpoint().getUrl()));
        result.setInProgress(true);

        try {
            searchClient.searchRetrieve(searchRequest, new ThrottledClient.SearchCallback() {
                @Override
                public void onSuccess(SRUSearchRetrieveResponse response, ThrottledClient.Stats stats) {
                    try {
                        statistics.addEndpointDatapoint(corpus.getInstitution(), corpus.getEndpoint(),
                                stats.getQueueTime(), stats.getExecutionTime());
                        result.addResponse(response);
                        List<Diagnostic> diagnostics = result.getDiagnostics();
                        if (diagnostics != null && !diagnostics.isEmpty()) {
                            log.error("diagnostic for url: {}", response.getRequest().getRequestedURI().toString());
                            for (Diagnostic diagnostic : diagnostics) {
                                statistics.addEndpointDiagnostic(corpus.getInstitution(), corpus.getEndpoint(),
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
                        statistics.addEndpointDatapoint(corpus.getInstitution(), corpus.getEndpoint(),
                                stats.getQueueTime(), stats.getExecutionTime());
                        statistics.addErrorDatapoint(corpus.getInstitution(), corpus.getEndpoint(), xc,
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
            log.error("SearchRetrieve error for " + corpus.getEndpoint().getUrl(), xc);
        }
    }

    public Long getId() {
        return id;
    }

    public List<Result> getResults(String corpusId) {
        List<Result> copy = new ArrayList<>();
        synchronized (results) {
            if (corpusId == null || corpusId.isEmpty()) {
                copy.addAll(results);
            } else {
                for (Result r : results) {
                    if (corpusId.equals(r.getCorpus().getId())) {
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
