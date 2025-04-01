package eu.clarin.sru.fcs.aggregator.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import eu.clarin.sru.fcs.aggregator.search.Search;

public class SearchCache {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(SearchCache.class);

    public final int DEFAULT_SEARCHES_SIZE_GC_THRESHOLD = 1000;
    public final int DEFAULT_SEARCHES_AGE_GC_THRESHOLD = 60; // minutes

    private final Map<String, Search> searches = Collections.synchronizedMap(new HashMap<>());

    public SearchCache() {
    }

    // ----------------------------------------------------------------------

    public Map<String, Search> getSearches() {
        return searches;
    }

    public Search getSearchById(String searchId) {
        return searches.get(searchId);
    }

    public void addSearch(Search search) {
        searches.put(search.getId(), search);
    }

    public List<String> gc(int searchesSizeThreshold, int searchesAgeThreshold) {
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

}
