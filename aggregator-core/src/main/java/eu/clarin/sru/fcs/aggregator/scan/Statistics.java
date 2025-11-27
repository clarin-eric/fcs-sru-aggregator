package eu.clarin.sru.fcs.aggregator.scan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stores statistics information about scans or searches. The info is then sent
 * to the JS client and displayed in the /Aggregator/stats page.
 * 
 * @author edima
 * @author ljo
 */
public class Statistics {

    public static class EndpointStats {

        /**
         * Represents a SRU Diagnostics with some context. It has a counter for repeated
         * occurrences.
         */
        public static class DiagPair {
            public final Diagnostic diagnostic;
            public final String context;
            public int counter;

            public DiagPair(Diagnostic diagnostic, String context, int counter) {
                this.diagnostic = diagnostic;
                this.context = context;
                this.counter = counter;
            }
        } // class DiagPair

        /**
         * Represents a Java exception with some context. It has a counter for repeated
         * occurrences.
         */
        public static class ExcPair {
            public final JavaException exception;
            public final String context;
            public int counter;

            public ExcPair(JavaException exception, String context, int counter) {
                this.exception = exception;
                this.context = context;
                this.counter = counter;
            }
        } // class ExcPair

        /**
         * Mini class to encapsulate a resource with its handle/id and title.
         */
        public static class ResourceInfo {
            public final String handle;
            public final String title;

            public boolean valid;
            public final List<String> notes = Collections.synchronizedList(new ArrayList<>());

            // TODO: handle special flags (e.g., access restricted) on the client side using
            // the handle to retrieve additional information on demand only

            public ResourceInfo(String handle, String title) {
                this.handle = handle;
                this.title = title;
                this.valid = true;
            }

            public ResourceInfo(String handle, String title, boolean valid, String note) {
                this.handle = handle;
                this.title = title;

                this.valid = valid;
                if (note != null && !note.isEmpty()) {
                    this.notes.add(note);
                }
            }
        } // class ResourceInfo

        // ------------------------------------------------------------------

        private final Object lock = new Object();

        FCSProtocolVersion version = FCSProtocolVersion.LEGACY;
        EnumSet<FCSSearchCapabilities> searchCapabilities = EnumSet.of(FCSSearchCapabilities.BASIC_SEARCH);

        final List<ResourceInfo> rootResources = new ArrayList<>();

        final List<Long> queueTimes = Collections.synchronizedList(new ArrayList<>());
        final List<Long> executionTimes = Collections.synchronizedList(new ArrayList<>());

        /**
         * The number of operations currently queued (started, not finished) at this
         * endpoint.
         */
        AtomicInteger numberOfRequestsActive = new AtomicInteger(0);

        int maxConcurrentRequests;

        final Map<String, DiagPair> diagnostics = Collections.synchronizedMap(new HashMap<>());
        final Map<String, ExcPair> errors = Collections.synchronizedMap(new HashMap<>());

        double avg(List<Long> q) {
            double sum = 0;
            for (long l : q) {
                sum += l;
            }
            return sum / q.size();
        }

        double max(List<Long> q) {
            double max = 0;
            for (long l : q) {
                max = max > l ? max : l;
            }
            return max;
        }

        public boolean hasQueueTime() {
            return !queueTimes.isEmpty();
        }

        public double getAvgQueueTime() {
            return avg(queueTimes) / 1000.0;
        }

        public double getMaxQueueTime() {
            return max(queueTimes) / 1000.0;
        }

        public boolean hasExecutionTime() {
            return !executionTimes.isEmpty();
        }

        public double getAvgExecutionTime() {
            return avg(executionTimes) / 1000.0;
        }

        public double getMaxExecutionTime() {
            return max(executionTimes) / 1000.0;
        }

        public int getMaxConcurrentRequests() {
            return maxConcurrentRequests;
        }

        public int getNumberOfRequests() {
            return executionTimes.size();
        }

        public int getNumberOfRequestsActive() {
            return numberOfRequestsActive.get();
        }

        public List<ResourceInfo> getRootResources() {
            return rootResources;
        }

        @Override
        public String toString() {
            return "EndpointStats [version=" + version + ", searchCapabilities=" + searchCapabilities
                    + ", rootResources=" + rootResources + ", diagnostics=" + diagnostics + ", errors=" + errors
                    + ", getAvgQueueTime()=" + getAvgQueueTime() + ", getMaxQueueTime()=" + getMaxQueueTime()
                    + ", getAvgExecutionTime()=" + getAvgExecutionTime() + ", getMaxExecutionTime()="
                    + getMaxExecutionTime() + ", getNumberOfRequests()=" + getNumberOfRequests() + "]";
        }

    } // class EndpointStats

    // ----------------------------------------------------------------------

    private final Object lock = new Object();

    final Date date = new Date();

    // institution to endpoint to statistics_per_endpoint map
    Map<String, Map<String, EndpointStats>> institutions = Collections
            .synchronizedMap(new HashMap<String, Map<String, EndpointStats>>());

    public Date getDate() {
        return date;
    }

    public Map<String, Map<String, EndpointStats>> getInstitutions() {
        return institutions;
    }

    public void initEndpoint(Institution institution, Endpoint endpoint, int maxConcurrentRequests) {
        EndpointStats stats = getEndpointStats(institution, endpoint);
        synchronized (stats.lock) {
            stats.maxConcurrentRequests = maxConcurrentRequests;
        }
    }

    public void incrementOperationsCount(Institution institution, Endpoint endpoint) {
        EndpointStats stats = getEndpointStats(institution, endpoint);
        stats.numberOfRequestsActive.incrementAndGet();
    }

    public void decrementOperationsCount(Institution institution, Endpoint endpoint) {
        EndpointStats stats = getEndpointStats(institution, endpoint);
        stats.numberOfRequestsActive.decrementAndGet();
    }

    public void addEndpointDatapoint(Institution institution, Endpoint endpoint, long enqueuedTime,
            long executionTime) {
        EndpointStats stats = getEndpointStats(institution, endpoint);
        synchronized (stats.lock) {
            stats.queueTimes.add(enqueuedTime);
            stats.executionTimes.add(executionTime);
        }
    }

    public void addEndpointDiagnostic(Institution institution, Endpoint endpoint, Diagnostic diag, String context) {
        EndpointStats stats = getEndpointStats(institution, endpoint);
        synchronized (stats.lock) {
            if (!stats.diagnostics.containsKey(diag.uri)) {
                stats.diagnostics.put(diag.uri, new EndpointStats.DiagPair(diag, context, 1));
            } else {
                stats.diagnostics.get(diag.uri).counter++;
            }
        }
    }

    public void addErrorDatapoint(Institution institution, Endpoint endpoint, Exception error, String context) {
        EndpointStats stats = getEndpointStats(institution, endpoint);
        JavaException jxc = new JavaException(error);
        synchronized (stats.lock) {
            if (!stats.errors.containsKey(jxc.message)) {
                stats.errors.put(jxc.message, new EndpointStats.ExcPair(jxc, context, 1));
            } else {
                stats.errors.get(jxc.message).counter++;
            }
        }
    }

    public void upgradeProtocolVersion(Institution institution, Endpoint endpoint) {
        EndpointStats stats = getEndpointStats(institution, endpoint);
        synchronized (stats.lock) {
            stats.version = endpoint.getProtocol().equals(FCSProtocolVersion.VERSION_2) ? FCSProtocolVersion.VERSION_2
                    : FCSProtocolVersion.VERSION_1;
            // also update search capabilities (related to version)
            stats.searchCapabilities = EnumSet.copyOf(endpoint.getSearchCapabilities());
        }
    }

    public void addEndpointResource(Institution institution, Endpoint endpoint, EndpointStats.ResourceInfo resource) {
        EndpointStats stats = getEndpointStats(institution, endpoint);
        synchronized (stats.lock) {
            stats.rootResources.add(resource);
        }
    }

    public void addEndpointResources(Institution institution, Endpoint endpoint,
            List<EndpointStats.ResourceInfo> resources) {
        EndpointStats stats = getEndpointStats(institution, endpoint);
        synchronized (stats.lock) {
            stats.rootResources.addAll(resources);
        }
    }

    private EndpointStats getEndpointStats(Institution institution, Endpoint endpoint) {
        EndpointStats stats;
        synchronized (lock) {
            if (!institutions.containsKey(institution.getName())) {
                institutions.put(institution.getName(),
                        Collections.synchronizedMap(new HashMap<String, EndpointStats>()));
            }
            Map<String, EndpointStats> esmap = institutions.get(institution.getName());
            if (!esmap.containsKey(endpoint.getUrl())) {
                final EndpointStats es = new EndpointStats();
                es.version = endpoint.getProtocol();
                es.searchCapabilities = EnumSet.copyOf(endpoint.getSearchCapabilities());
                esmap.put(endpoint.getUrl(), es);
            }
            stats = esmap.get(endpoint.getUrl());
        }
        return stats;
    }

} // class Statistics
