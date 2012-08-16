package clarind.fcs;


import eu.clarin.sru.client.*;

import eu.clarin.sru.fcs.ClarinFederatedContentSearchRecordData;
import eu.clarin.sru.fcs.ClarinFederatedContentSearchRecordParser;

import java.util.*;
public class TestSRUClient1 {
    
    public static void main (String[] args) throws Exception {
        
        String url = "http://weblicht.sfs.uni-tuebingen.de/rws/cqp-ws/cqp/sru";
        
        SRUClient client = new SRUClient(SRUVersion.VERSION_1_2);
        
        client.registerRecordParser(new ClarinFederatedContentSearchRecordParser());
        
        SRUDefaultHandlerAdapter handler = new SRUDefaultHandlerAdapter();
        
        SRUExplainRequest request = new SRUExplainRequest(url);
        client.explain(request, handler);
        
        
        
    } // main
    
}
