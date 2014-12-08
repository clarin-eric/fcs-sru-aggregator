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
	// queue of operations waiting for execution
	private final Map<URI, Queue<Operation>> endpoint2exec = new HashMap<URI, Queue<Operation>>();
	private final int nowExecuting = 0;
	private final Object lock = new Object();

	GenericClient(SRUThreadedClient sruClient) {
		this.sruClient = sruClient;
	}

	void execute(URI endpoint, Operation op) {
		op.setClient(this);
		synchronized (lock) {
			if (!endpoint2exec.containsKey(endpoint)) {
				endpoint2exec.put(endpoint, new ArrayDeque<Operation>());
			}
			op.stats().enqueuedTime = System.currentTimeMillis();
			endpoint2exec.get(endpoint).add(op);
			executeNextOperationOfEndpoint(endpoint);
		}
	}

	void executeNextOperationOfEndpoint(URI endpoint) {
		synchronized (lock) {
			Queue<Operation> queue = endpoint2exec.get(endpoint);
			if (queue == null || queue.isEmpty()) {
				return;
			}
			if (nowExecuting >= ThrottledClient.MAX_CONCURRENT_REQUESTS) {
				return;
			}
			Operation op = queue.poll();
			op.stats().startedTime = System.currentTimeMillis();
			op.execute(sruClient);
		}
	}
}
