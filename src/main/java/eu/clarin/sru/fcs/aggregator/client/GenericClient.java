package eu.clarin.sru.fcs.aggregator.client;

import eu.clarin.sru.client.SRUThreadedClient;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

/**
 *
 * @author edima
 */
class GenericClient {
	private final SRUThreadedClient sruClient;
	public final int maxConcurrentRequests;
	// queue of operations waiting for execution

	static class ExecQueue {

		int nowExecuting = 0;
		Queue<Operation> queue = new ArrayDeque<>();
	}
	private final Map<URI, ExecQueue> endpointMap = new HashMap<URI, ExecQueue>();
	private final Object lock = new Object();

	GenericClient(SRUThreadedClient sruClient, int maxConcurrentRequests) {
		this.sruClient = sruClient;
		this.maxConcurrentRequests = maxConcurrentRequests;
	}

	void execute(URI endpoint, Operation op) {
		op.setClient(this);
		synchronized (lock) {
			ExecQueue eq = endpointMap.get(endpoint);
			if (eq == null) {
				eq = new ExecQueue();
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
			if (eq.nowExecuting >= maxConcurrentRequests) {
				return;
			}
			eq.nowExecuting++;
			Operation op = eq.queue.poll();
			op.stats().startedTime = System.currentTimeMillis();
			op.execute(sruClient);
		}
	}
}
