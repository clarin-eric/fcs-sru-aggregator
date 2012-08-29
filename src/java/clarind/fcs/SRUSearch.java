package clarind.fcs;

import eu.clarin.sru.client.*;

import eu.clarin.sru.fcs.ClarinFederatedContentSearchRecordData;
import eu.clarin.sru.fcs.ClarinFederatedContentSearchRecordParser;
//import org.xml.sax.helpers.DefaultHandler;

import org.zkoss.zul.Row;
import org.zkoss.zul.Label;

import java.util.*;

public class SRUSearch {

    private ArrayList<Row> zeilen;

    public ArrayList<Row> execute(String query, String endpointURL, String corpus, int maximumRecords) throws Exception {
        zeilen  = new ArrayList<Row>();

        SRUClient client = new SRUClient();
        client.registerRecordParser(new ClarinFederatedContentSearchRecordParser());

        SRUSearchRetrieveRequest request = new SRUSearchRetrieveRequest(endpointURL);
        request.setQuery(query);
        request.setRecordSchema(ClarinFederatedContentSearchRecordParser.FCS_RECORD_SCHEMA);
        request.setMaximumRecords(maximumRecords);

        if (corpus != null) {
            request.setExtraRequestData("x-context", corpus);
            System.out.println("I'm setting the x-context");

        }
        try {
        client.searchRetrieve(request, new SRUDefaultHandlerAdapter() {
            @Override
            public void onRecord(String identifier, int position, SRURecordData data) throws SRUClientException {
                if (ClarinFederatedContentSearchRecordParser.FCS_NS.equals(data.getRecordSchema())) {
                    ClarinFederatedContentSearchRecordData record = (ClarinFederatedContentSearchRecordData) data;
                    String left = record.getLeft();
                    String hit = record.getKeyword();
                    String right = record.getRight();

//                    System.out.print("LEFT: " + left);
//                    System.out.println("HIT: " + hit);
//                    System.out.println("RIGHT: " + right);

                    Row r = new Row();
                    r.appendChild(new Label(left));
                    r.appendChild(new Label(hit));
                    r.appendChild(new Label(right));

                    zeilen.add(r);
                }
            }
        });
        } catch (Exception ex){
            System.out.println(ex.getMessage());
        }

        
//        System.out.println("NUMBER of LINES: " + zeilen.size());
        
        return zeilen;
    }

    public static void main(String[] args) throws Exception {
    } // main
}
