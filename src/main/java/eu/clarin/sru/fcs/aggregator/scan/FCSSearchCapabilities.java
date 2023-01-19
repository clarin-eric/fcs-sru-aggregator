package eu.clarin.sru.fcs.aggregator.scan;

/**
 * Endpoint description capabilities. Used to filter which corpus/endpoint
 * supports what type of search. E.g. allows use cases where endpoints support
 * FCS 2.0 without FCS-QL searches.
 *
 * @author koerner
 */
public enum FCSSearchCapabilities {
    BASIC_SEARCH, // basic search (required), cql?
    ADVANCED_SEARCH, // FCS 2.0 advanced search
}
