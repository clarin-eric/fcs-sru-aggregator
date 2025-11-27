package eu.clarin.sru.fcs.aggregator.app.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import eu.clarin.sru.fcs.aggregator.scan.Statistics;
import eu.clarin.sru.fcs.aggregator.scan.Statistics.EndpointStats;
import eu.clarin.sru.fcs.aggregator.search.Result;
import eu.clarin.sru.fcs.aggregator.search.Search;

public class SearchJobStatistics {

    public static class EndpointStatsInfo {
        int numRequestsTotal;
        double avgQueueTime;
        double avgExecutionTime;

        int numInProgress;
    }

    // ----------------------------------------------------------------------

    @JsonProperty(value = "status", required = true)
    public String getStatus() {
        return numRequestsCancelled > 0
                // if any cancelled, then all were cancelled (only waiting)
                ? "cancelled"
                : numRequestsInProgress > 0
                        // if some still in progress, then search is running
                        ? "in-progress"
                        : numRequests == numRequestsFinished
                                // if finished (and not cancelled), then we are done
                                ? "done"
                                // else, who knows
                                : "unknown";
    }

    @JsonProperty(required = true)
    long startDate;

    @JsonProperty(required = true)
    int numRequests;

    @JsonProperty(required = true)
    int numRequestsCancelled;

    @JsonProperty(required = true)
    int numRequestsInProgress;

    @JsonProperty(required = true)
    int numRequestsFinished;

    @JsonProperty(required = false)
    double expectedTimeToCompletion;

    @JsonProperty(required = false)
    Map<String, Double> expectedTimeToCompletionPerEndpoint;

    @JsonProperty(required = false)
    int numSearchesBefore;

    @JsonProperty(required = false)
    int numRequestsBefore;

    @JsonProperty(required = false)
    Map<String, Double> expectedTimeToCompletionPerEndpointBefore;

    // ----------------------------------------------------------------------

    public SearchJobStatistics() {
    }

    public static SearchJobStatistics fromSearch(Search search, Statistics searchStats,
            List<Search> otherActiveSearches, Map<String, Integer> endpointsWithNumRequests) {
        final int numResources = search.getNumberOfResources();
        final int numCancelled = search.getNumberOfResourcesCancelled();
        final int numInProgress = search.getNumberOfResourcesInProgress();
        final int numFinished = numResources - numCancelled - numInProgress;

        SearchJobStatistics stats = new SearchJobStatistics();

        // status
        stats.startDate = search.getCreatedAt();

        // stats about requests (-> resources)
        stats.numRequests = numResources;
        stats.numRequestsCancelled = numCancelled;
        stats.numRequestsInProgress = numInProgress;
        stats.numRequestsFinished = numFinished;

        stats.numSearchesBefore = otherActiveSearches.size();

        if (numCancelled == 0 && numInProgress > 0) {
            // if not cancelled and some are in progress, we can computed how many are
            // live and queued etc. and how long it might take

            // endpoint url -> endpoint stats
            Map<String, EndpointStats> endpoints = new HashMap<>();
            searchStats.getInstitutions().values().forEach((e) -> endpoints.putAll(e));

            // endpoint url -> list of results
            Map<String, List<Result>> endpointWithResults = new HashMap<>();
            for (Result result : search.getResults()) {
                // only those resources that are still WIP
                if (result.getInProgress()) {
                    String endpointUrl = result.getEndpointUrl();

                    // store per endpoint url
                    if (!endpointWithResults.containsKey(endpointUrl)) {
                        endpointWithResults.put(endpointUrl, new ArrayList<>());
                    }
                    endpointWithResults.get(endpointUrl).add(result);
                }
            }

            Map<String, Double> endpointsExpectedTime = new HashMap<>(endpointWithResults.size());
            Map<String, Double> endpointsExpectedTimeBefore = new HashMap<>(endpointWithResults.size());
            for (Map.Entry<String, List<Result>> entry : endpointWithResults.entrySet()) {
                String endpointUrl = entry.getKey();

                EndpointStats endpointStats = endpoints.get(endpointUrl);
                if (endpointStats == null) {
                    // NOTE: should not happen but could be due to no searches existing?
                    continue;
                }

                double expectedTime = 0.0;

                if (endpointStats.hasExecutionTime()) {
                    int numOfParallelRequests = endpointStats.getMaxConcurrentRequests();
                    final double avgExecTime = endpointStats.getAvgExecutionTime();

                    // offset by searches/requests that were submitted before
                    int numRequestsBefore = endpointsWithNumRequests.getOrDefault(endpointUrl, 0);
                    if (numRequestsBefore > 0) {
                        double expectedTimeBefore = avgExecTime * numRequestsBefore / numOfParallelRequests;
                        endpointsExpectedTimeBefore.put(endpointUrl, expectedTimeBefore);
                        expectedTime += expectedTimeBefore;
                    }

                    // these results are already filtered to only be inProgress=1
                    List<Result> results = entry.getValue();
                    final int numRequests = results.size();

                    if (numRequests <= numOfParallelRequests) {
                        expectedTime += avgExecTime;
                    } else {
                        int currentlyExecutingMaybe = Math.min(numRequests, numOfParallelRequests);
                        int currentlyQueuedProbably = numRequests - currentlyExecutingMaybe;

                        // time for currentlyExecutingMaybe (execution)
                        expectedTime += avgExecTime;

                        // time for currentlyQueuedProbably (queued + execution)
                        // without queue time (since staggered, this might be too confusing and summed
                        // execution times should be enough?)
                        expectedTime += avgExecTime * currentlyQueuedProbably / numOfParallelRequests;
                    }

                    endpointsExpectedTime.put(endpointUrl, expectedTime);
                } else {
                    endpointsExpectedTime.put(endpointUrl, -1.0);
                }

            }

            // per endpoint, avg execution time for batched requests
            stats.expectedTimeToCompletionPerEndpointBefore = endpointsExpectedTimeBefore;
            // from the active searches, sum only those where endpoints are used in the
            // current search (ignore others)
            stats.numRequestsBefore = endpointsWithNumRequests.entrySet().stream()
                    .filter(e -> endpointsExpectedTimeBefore.containsKey(e.getKey()))
                    .map(e -> e.getValue()).mapToInt(Integer::intValue).sum();

            // per endpoint, avg execution time for batched requests (current search)
            stats.expectedTimeToCompletionPerEndpoint = endpointsExpectedTime;
            stats.expectedTimeToCompletion = endpointsExpectedTime.values().stream().filter(v -> v >= 0.0)
                    .reduce(0.0, (a, b) -> a > b ? a : b).floatValue();
        }

        return stats;
    }

} // class SearchJobStatistics
