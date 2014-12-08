package eu.clarin.sru.fcs.aggregator.client;

/**
 *
 * @author edima
 */
class OpStats implements ThrottledClient.Stats {
	long enqueuedTime = 0;
	long startedTime = 0;
	long finishedTime = 0;

	@Override
	public long getQueueTime() {
		return startedTime - enqueuedTime;
	}

	@Override
	public long getExecutionTime() {
		return finishedTime - startedTime;
	}

}
