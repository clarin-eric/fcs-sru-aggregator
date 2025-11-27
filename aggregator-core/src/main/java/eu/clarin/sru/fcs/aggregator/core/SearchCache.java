package eu.clarin.sru.fcs.aggregator.core;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.LoggerFactory;

import eu.clarin.sru.fcs.aggregator.search.Search;

public class SearchCache {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(SearchCache.class);

    public final int DEFAULT_SEARCHES_SIZE_GC_THRESHOLD = 1000;
    public final int DEFAULT_SEARCHES_AGE_GC_THRESHOLD = 60; // minutes

    private final Map<String, Search> searches = Collections.synchronizedMap(new HashMap<>());
    private AtomicInteger numberOfSearches = new AtomicInteger(0);

    public SearchCache() {
    }

    // ----------------------------------------------------------------------

    /**
     * Get all cached searches.
     * 
     * @return mapping of search id to search object
     */
    public Map<String, Search> getSearches() {
        // TODO: make readonly?
        return searches;
    }

    public Map<String, Search> getSearches(Duration maxAge) {
        return getSearches(maxAge.getSeconds());
    }

    /**
     * Get filtered subset of cached searches that are younger than
     * <code>maxAgeSeconds</code>.
     * 
     * @param maxAgeSeconds the maximum age of the search (in seconds)
     * @return searches younger than max age
     */
    public Map<String, Search> getSearches(long maxAgeSeconds) {
        Map<String, Search> searchesFiltered = new HashMap<>();

        long t0 = System.currentTimeMillis();
        for (Map.Entry<String, Search> e : searches.entrySet()) {
            long dtsec = (t0 - e.getValue().getCreatedAt()) / 1000L;
            if (dtsec <= maxAgeSeconds) {
                searchesFiltered.put(e.getKey(), e.getValue());
            }
        }

        return searchesFiltered;
    }

    public Search getSearchById(String searchId) {
        return searches.get(searchId);
    }

    public void addSearch(Search search) {
        searches.put(search.getId(), search);
        numberOfSearches.incrementAndGet();
    }

    public List<String> gc(int searchesSizeThreshold, long searchesAgeThreshold) {
        List<String> toBeRemoved = new ArrayList<>();
        if (searches.size() > searchesSizeThreshold) {
            long t0 = System.currentTimeMillis();
            for (Map.Entry<String, Search> e : searches.entrySet()) {
                long dtmin = (t0 - e.getValue().getCreatedAt()) / 1000 / 60;
                if (dtmin > searchesAgeThreshold) {
                    log.info("removing search {}: {} minutes old", e.getKey(), dtmin);
                    toBeRemoved.add(e.getKey());
                }
            }
            for (String searchId : toBeRemoved) {
                searches.remove(searchId);
            }
        }
        return toBeRemoved;
    }

    // ----------------------------------------------------------------------

    /**
     * Total number of searches since startup.
     * 
     * @return the number of searches
     */
    public int getNumberOfSearches() {
        return numberOfSearches.get();
    }

    /**
     * The number of searches that are still active. (In cache, either finished,
     * cancelled or in progress.)
     * 
     * @param maxAge         the duration (a timespan) of how old the searches
     *                       should at
     *                       most be
     * @param onlyInProgress whether the search is in process (not done/cancelled)
     *                       or all
     * @return the number of cached searches fulfilling the filter criteria
     */
    public int getNumberOfSearches(Duration maxAge, boolean onlyInProgress) {
        return getNumberOfSearches(maxAge.getSeconds(), onlyInProgress);
    }

    /**
     * The number of searches that are still active. (In cache, either finished,
     * cancelled or in progress.)
     * 
     * @param maxAgeSeconds  the maximum number of seconds limiting the age of the
     *                       search
     * @param onlyInProgress whether the search is in process (not done/cancelled)
     *                       or all
     * @return the number of cached searches fulfilling the filter criteria
     */
    public int getNumberOfSearches(long maxAgeSeconds, boolean onlyInProgress) {
        long t0 = System.currentTimeMillis();
        int numberOfSearches = 0;
        for (Search search : searches.values()) {
            long dtsec = (t0 - search.getCreatedAt()) / 1000L;
            if (dtsec <= maxAgeSeconds) {
                // either all or in-progress
                if (!onlyInProgress || (onlyInProgress && !search.isFinished())) {
                    numberOfSearches++;
                }
            }
        }
        return numberOfSearches;
    }

}
