package eu.clarin.sru.fcs.aggregator.util;

/**
 * Utility for storing constants related to SRU/CQL specification.
 * 
 * @author Yana Panchenko
 */
public class SRUCQL {
    
    public static final String OPERATION = "operation";
    
    public static final String VERSION = "version";
    
    
    public static final String SEARCH_RETRIEVE = "searchRetrieve";
    public static final String SEARCH_CORPUS_HANDLE_PARAMETER = "x-cmd-context";
    public static final String SEARCH_QUERY_PARAMETER = "query";
    
    
    public static final String SCAN = "scan";
    public static final String SCAN_RESOURCE_PARAMETER = "fcs.resource";
    public static final String SCAN_RESOURCE_PARAMETER_DEFAULT_VALUE = "root";
    public static final String SCAN_RESOURCE_INFO_PARAMETER = "x-cmd-resource-info";
    public static final String SCAN_RESOURCE_INFO_PARAMETER_DEFAULT_VALUE = "true";
    
    
    public static final String EXPLAIN = "explain";
    
    public static final String AGGREGATION_CONTEXT = "x-aggregation-context";
    
    
}
