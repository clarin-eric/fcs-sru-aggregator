package clarind.fcs;

import eu.clarin.sru.client.*;

import eu.clarin.sru.fcs.ClarinFederatedContentSearchRecordData;
import eu.clarin.sru.fcs.ClarinFederatedContentSearchRecordParser;
import org.xml.sax.helpers.DefaultHandler;

import org.zkoss.zul.Html;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Row;
import org.zkoss.zul.Label;


public class SRUSearch {
    private StringBuilder sb;
    
    public  StringBuilder execute(String query, String endpointURL, String corpus, int maximumRecords, Html results, final Grid grid) throws Exception {
        sb = new StringBuilder();
        sb.append("URL: "+ endpointURL + "<br>");
        sb.append("CORPUS: " + corpus + "<br><hr>");
        
        
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

        client.searchRetrieve(request, new SRUDefaultHandlerAdapter() {
            @Override
            public void onRecord(String identifier, int position, SRURecordData data) throws SRUClientException {
                if (ClarinFederatedContentSearchRecordParser.FCS_NS.equals(data.getRecordSchema())) {
                    ClarinFederatedContentSearchRecordData record = (ClarinFederatedContentSearchRecordData) data;
                    String left = record.getLeft();
                    String hit = record.getKeyword();
                    String right = record.getRight();
                    
                    System.out.print("LEFT: " + left);
                    System.out.println("HIT: " + hit);
                    System.out.println("RIGHT: " + right);
                    
                    sb.append(left);
                    sb.append("<font color='red'>");
                    sb.append(" " + hit + " ");
                    sb.append("</font>");
                    sb.append(right);
                    sb.append("<br><br>");
                    
//                    Rows rows = grid.getRows();
//                    Row r = new Row();
//                    r.appendChild(new Label(left));
//                    r.appendChild(new Label(hit));
//                    r.appendChild(new Label(right));
//                    
//                    rows.appendChild(r);
                    
                    
                }
            }
        });
        sb.append("<hr>");
        return sb;
    }

    public static void main(String[] args) throws Exception {

    


    } // main
}
