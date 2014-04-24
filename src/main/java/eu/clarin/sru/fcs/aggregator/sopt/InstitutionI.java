package eu.clarin.sru.fcs.aggregator.sopt;

import java.util.*;

/**
 * Institution. Can have Endpoint children.
 * 
 * @author Yana Panchenko
 */
    public interface InstitutionI {

    
    public Endpoint add(String endpointUrl);

    public String getName();
    
    public String getLink();
    
   public List<Endpoint> getEndpoints();
   
    public Endpoint getEndpoint(int index);
    
    public Endpoint getEndpoint(String endpointUrl);
}
