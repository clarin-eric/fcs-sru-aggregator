package eu.clarin.sru.fcs.aggregator.client;

/**
 * @author ekoerner
 */
public interface CancellableOperation<Request, Response> extends Operation<Request, Response> {

    void cancel();

    boolean isCancelled();

} // interface CancellableOperation
