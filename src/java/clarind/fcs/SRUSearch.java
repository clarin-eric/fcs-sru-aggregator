package clarind.fcs;

import eu.clarin.sru.client.*;

//import eu.clarin.sru.fcs.ClarinFederatedContentSearchRecordData;
//import eu.clarin.sru.fcs.ClarinFederatedContentSearchRecordParser;
//import org.xml.sax.helpers.DefaultHandler;

import org.zkoss.zul.Row;
import org.zkoss.zul.Label;

import java.util.*;

public class SRUSearch {

    private ArrayList<Row> zeilen;

    public ArrayList<Row> execute(String query, String endpointURL, String corpus, int maximumRecords) throws Exception {
//        zeilen  = new ArrayList<Row>();
//
//        SRUSimpleClient client = new SRUSimpleClient();
//        client.registerRecordParser(new ClarinFederatedContentSearchRecordParser());
//
//        SRUSearchRetrieveRequest request = new SRUSearchRetrieveRequest(endpointURL);
//        request.setQuery(query);
//        request.setRecordSchema(ClarinFederatedContentSearchRecordData.RECORD_SCHEMA);
//        request.setMaximumRecords(maximumRecords);
//
//        if (corpus != null) {
//            request.setExtraRequestData("x-context", corpus);
//        }
//        
//        try {
//            
//        client.searchRetrieve(request, new SRUDefaultHandlerAdapter() {
//            @Override
//            public void onRecord(String identifier, int position, SRURecordData data) throws SRUClientException {
//               // if (ClarinFederatedContentSearchRecordParser.FCS_NS.equals(data.getRecordSchema())) {
//                    ClarinFederatedContentSearchRecordData record = (ClarinFederatedContentSearchRecordData) data;
//                    String left = record.getLeft();
//                    String hit = record.getKeyword();
//                    String right = record.getRight();
//
//                    Row r = new Row();
//                    r.appendChild(new Label(left));
//                    Label l = new Label(hit);
//                    l.setStyle("color:#8f3337;");
//                    r.appendChild(l);
//                    r.appendChild(new Label(right));
//
//                    zeilen.add(r);
//                //}
//            }
//        });
//        } catch (Exception ex){
//            System.out.println(ex.getMessage());
//        }
        
        return zeilen;
    }

    public static void main(String[] args) throws Exception {
    } // main
}
