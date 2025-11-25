package eu.clarin.sru.fcs.aggregator.client;

import eu.clarin.sru.client.SRUThreadedClient;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import org.slf4j.LoggerFactory;

/**
 * This is a SRUThreadedClient wrapper that only allows a limited number of
 * requests per endpoint, no matter what SRU operation the requests make.
 * This class is an internal utility class, used by the ThrottledClient class.
 * 
 * @author edima
 */
class GenericClient {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(GenericClient.class);

    private final SRUThreadedClient sruClient;
    final MaxConcurrentRequestsCallback maxConcurrentRequestsCallback;

    /**
     * queue of operations waiting for execution
     */
    static class ExecQueue {
        int maxConcurrentRequests = 0;
        int nowExecuting = 0;
        Queue<Operation<?, ?>> queue = new ArrayDeque<>();
    }

    private final Map<URI, ExecQueue> endpointMap = new HashMap<URI, ExecQueue>();
    private final Object lock = new Object();

    GenericClient(SRUThreadedClient sruClient, MaxConcurrentRequestsCallback maxConcurrentRequestsCallback) {
        this.sruClient = sruClient;
        this.maxConcurrentRequestsCallback = maxConcurrentRequestsCallback;
    }

    // ----------------------------------------------------------------------

    void execute(URI endpoint, Operation<?, ?> op) {
        op.setClient(this);
        synchronized (lock) {
            ExecQueue eq = endpointMap.get(endpoint);
            if (eq == null) {
                eq = new ExecQueue();
                try {
                    eq.maxConcurrentRequests = maxConcurrentRequestsCallback.getMaxConcurrentRequest(endpoint);
                    log.debug("CONCURRENCY LEVEL {} for operation: {} for endpoint: {}", eq.maxConcurrentRequests,
                            op.getClass().getSimpleName(), endpoint);
                } catch (Exception xc) {
                    // ignore
                } finally {
                    if (eq.maxConcurrentRequests <= 0) {
                        eq.maxConcurrentRequests = 1;
                    }
                }
                endpointMap.put(endpoint, eq);
            }

            op.stats().enqueuedTime = System.currentTimeMillis();
            eq.queue.add(op);
            eq.nowExecuting++; // counter the following decrement in executeNext
            executeNextOperationOfEndpoint(endpoint);
        }
    }

    void executeNextOperationOfEndpoint(URI endpoint) {
        synchronized (lock) {
            ExecQueue eq = endpointMap.get(endpoint);

            eq.nowExecuting--; // assume an operation just finished
            if (eq.queue.isEmpty()) {
                return;
            }
            if (eq.nowExecuting >= eq.maxConcurrentRequests) {
                return;
            }
            eq.nowExecuting++;

            Operation<?, ?> op = eq.queue.poll();
            op.stats().startedTime = System.currentTimeMillis();
            op.execute(sruClient);
        }
    }

    // ----------------------------------------------------------------------

    public void shutdown() {
        sruClient.shutdown();
    }

    public void shutdownNow() {
        sruClient.shutdownNow();
    }

} // class GenericClient
