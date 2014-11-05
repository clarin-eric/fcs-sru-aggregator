package eu.clarin.sru.fcs.aggregator.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author edima
 */
public class CounterLatch {
	private final AtomicInteger counter = new AtomicInteger();
	private final Object lock = new Object();

	public int get() {
		return counter.get();
	}

	public int increment() {
		return counter.incrementAndGet();
	}

	public int decrement() {
		synchronized (lock) {
			int ret = counter.decrementAndGet();
			if (ret <= 0) {
				lock.notifyAll();
			}
			return ret;
		}
	}

	public void await() throws InterruptedException {
		synchronized (lock) {
			if (0 == counter.get()) {
				return;
			}
			lock.wait();
		}
	}
}
