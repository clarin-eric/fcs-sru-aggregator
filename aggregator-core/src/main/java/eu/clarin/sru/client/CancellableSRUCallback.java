package eu.clarin.sru.client;

public interface CancellableSRUCallback<V extends SRUAbstractRequest, S extends SRUAbstractResponse<V>>
        extends SRUCallback<V, S> {

    /**
     * Invoked when the request has been cancelled.
     *
     * @param request
     *                the original request
     */
    public void onCancelled(V request);

} // interface CancellableSRUCallback
