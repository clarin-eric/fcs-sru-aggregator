package eu.clarin.sru.fcs.aggregator.client;

import eu.clarin.sru.client.SRUThreadedClient;

/**
 * @author edima
 */
interface Operation<Request, Response> {

    void setClient(GenericClient client);

    void execute(SRUThreadedClient sruClient);

    OperationStats stats();
}
