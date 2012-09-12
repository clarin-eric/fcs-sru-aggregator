package clarind.fcs;

import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRURecord;
import eu.clarin.sru.client.SRUSearchRetrieveRequest;
import eu.clarin.sru.client.SRUSearchRetrieveResponse;
import eu.clarin.sru.client.SRUSurrogateRecordData;
import eu.clarin.sru.client.SRUThreadedClient;
import eu.clarin.sru.fcs.ClarinFederatedContentSearchRecordData;
import eu.clarin.sru.fcs.ClarinFederatedContentSearchRecordParser;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import org.apache.log4j.Level;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SRUSearchThreaded {

    private static volatile SRUSearchThreaded instance = null;
    private SRUThreadedClient client;
    private ArrayList<Row> zeilen;
    private static final Logger logger = LoggerFactory.getLogger("FCS-SRUSEARCH");

    private SRUSearchThreaded() {


        org.apache.log4j.BasicConfigurator.configure(
                new org.apache.log4j.ConsoleAppender(
                new org.apache.log4j.PatternLayout("%-5p [%t] %m%n"),
                org.apache.log4j.ConsoleAppender.SYSTEM_ERR));
        org.apache.log4j.Logger logger =
                org.apache.log4j.Logger.getRootLogger();
        logger.setLevel(org.apache.log4j.Level.INFO);
        logger.getLoggerRepository().getLogger("FCS-SRUSEARCH").setLevel(
                org.apache.log4j.Level.INFO);




        this.client = new SRUThreadedClient();
        System.out.println("GOT A CLIENT");
        try {
            client.registerRecordParser(new ClarinFederatedContentSearchRecordParser());
        } catch (SRUClientException e) {
            System.out.println(e.getMessage());
        }
    }

    public void shutdown() {
        client.shutdown();
    }

    public static SRUSearchThreaded getInstance() {
        if (instance == null) {
            synchronized (SRUSearchThreaded.class) {
                if (instance == null) {
                    instance = new SRUSearchThreaded();
                }
            }
        }
        return instance;
    } // getInstance

    public ArrayList<Row> execute(String query, String endpointURL, String corpus, int maximumRecords) throws Exception {
        zeilen = new ArrayList<Row>();
        System.out.println("EXECUTING SEARCH");
    
        SRUSearchRetrieveRequest request = new SRUSearchRetrieveRequest(endpointURL);
        request.setMaximumRecords(maximumRecords);
        request.setRecordSchema(ClarinFederatedContentSearchRecordData.RECORD_SCHEMA);
        request.setQuery(query);

        if (corpus != null) {
            request.setExtraRequestData("x-context", corpus);
        }

        Future<SRUSearchRetrieveResponse> result = client.searchRetrieve(request);

        for (SRURecord record : result.get().getRecords()) {
            if (record.isRecordSchema(ClarinFederatedContentSearchRecordData.RECORD_SCHEMA)) {
                ClarinFederatedContentSearchRecordData r =
                        (ClarinFederatedContentSearchRecordData) record.getRecordData();
                Row row = new Row();

                row.appendChild(new Label(r.getLeft()));
                Label l = new Label(r.getKeyword());
                l.setStyle("color:#8f3337;");
                row.appendChild(l);
                row.appendChild(new Label(r.getRight()));

                zeilen.add(row);

            } else if (record.isRecordSchema(SRUSurrogateRecordData.RECORD_SCHEMA)) {
                SRUSurrogateRecordData r =
                        (SRUSurrogateRecordData) record.getRecordData();

            } else {
                System.out.println("Unknown record schema");
            }
        } // for record
        return zeilen;
    }
}
