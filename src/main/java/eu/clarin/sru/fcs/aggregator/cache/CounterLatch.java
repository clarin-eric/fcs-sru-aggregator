package eu.clarin.sru.fcs.aggregator.cache;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;

/**
 *
 * @author edima
 */
public class CounterLatch {

	private static final IntUnaryOperator countDown = new IntUnaryOperator() {
		@Override
		public int applyAsInt(int operand) {
			return (operand > 0) ? operand - 1 : 0;
		}
	};

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
			int ret = counter.updateAndGet(countDown);
			if (0 == ret) {
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
