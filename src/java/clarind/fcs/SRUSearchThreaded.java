package clarind.fcs;

import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRURecord;
import eu.clarin.sru.client.SRUSearchRetrieveRequest;
import eu.clarin.sru.client.SRUSearchRetrieveResponse;
import eu.clarin.sru.client.SRUSurrogateRecordData;
import eu.clarin.sru.client.SRUThreadedClient;
import eu.clarin.sru.fcs.ClarinFCSRecordData;
import eu.clarin.sru.fcs.ClarinFCSRecordParser;
import eu.clarin.sru.fcs.DataView;
import eu.clarin.sru.fcs.KWICDataView;
import eu.clarin.sru.fcs.Resource;
import java.util.ArrayList;
import java.util.List;
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
        logger.setLevel(org.apache.log4j.Level.DEBUG);
        logger.getLoggerRepository().getLogger("FCS-SRUSEARCH").setLevel(
                org.apache.log4j.Level.DEBUG);

        this.client = new SRUThreadedClient();
        System.out.println("GOT A CLIENT");
        try {
            client.registerRecordParser(new ClarinFCSRecordParser());
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
        request.setRecordSchema(ClarinFCSRecordData.RECORD_SCHEMA);
        request.setQuery(query);

        if (corpus != null) {
            request.setExtraRequestData("x-context", corpus);
        }

        Future<SRUSearchRetrieveResponse> result = client.searchRetrieve(request);

        //  printSearchResponse(result.get());

        if (result.get().hasRecords()) {
            for (SRURecord record : result.get().getRecords()) {
                logger.info("schema = {}, identifier = {}, position = {}",
                        new Object[]{record.getRecordSchema(),
                            record.getRecordIdentifier(),
                            record.getRecordPosition()});
                if (record.isRecordSchema(ClarinFCSRecordData.RECORD_SCHEMA)) {
                    ClarinFCSRecordData rd =
                            (ClarinFCSRecordData) record.getRecordData();
                    // dumpResource(rd.getResource());
                    Resource resource = rd.getResource();
                    if (resource.hasDataViews()) {
                        zeilen = dataViews2Rows(resource.getDataViews());
                        //dumpDataView("CLARIN-FCS: ", resource.getDataViews());
                    }
                    if (resource.hasResourceFragments()) {
                        for (Resource.ResourceFragment fragment : resource.getResourceFragments()) {
                            logger.debug("CLARIN-FCS: ResourceFragment: pid={}, ref={}",
                                    fragment.getPid(), fragment.getRef());
                            if (fragment.hasDataViews()) {
                                zeilen.addAll(dataViews2Rows(fragment.getDataViews()));
                                //dumpDataView("CLARIN-FCS: ResourceFragment/", fragment.getDataViews());
                            }
                        }
                    }

                } else if (record.isRecordSchema(SRUSurrogateRecordData.RECORD_SCHEMA)) {
                    SRUSurrogateRecordData r =
                            (SRUSurrogateRecordData) record.getRecordData();
                    logger.info("SURROGATE DIAGNOSTIC: uri={}, message={}, detail={}",
                            new Object[]{r.getURI(), r.getMessage(),
                                r.getDetails()});
                } else {
                    logger.info("UNSUPPORTED SCHEMA: {}",
                            record.getRecordSchema());
                }
            }
        } else {
            logger.info("no results");
        }


//        for (SRURecord record : result.get().getRecords()) {
//            if (record.isRecordSchema(ClarinFCSRecordData.RECORD_SCHEMA)) {
//                ClarinFCSRecordData r =
//                        (ClarinFCSRecordData) record.getRecordData();
//                Row row = new Row();
//                
//                Label toTheLeft = new Label();
//                toTheLeft.setMultiline(true);                
//               // toTheLeft.setValue(r.getLeft());
//                toTheLeft.setSclass("word-wrap");
//                
//                row.appendChild(toTheLeft);
//                //Label l = new Label(r.getKeyword());
//                //l.setStyle("color:#8f3337;");
//                //l.setMultiline(true);
//               // l.setSclass("word-wrap");
//                //row.appendChild(l);
//                
//                 Label toTheRight = new Label();
//                toTheRight.setMultiline(true);
//                toTheRight.setSclass("word-wrap");
//              //  toTheRight.setValue(r.getRight());
//                
//                row.appendChild(toTheRight);
//
//                zeilen.add(row);
//
//            } else if (record.isRecordSchema(SRUSurrogateRecordData.RECORD_SCHEMA)) {
//                SRUSurrogateRecordData r =
//                        (SRUSurrogateRecordData) record.getRecordData();
//
//            } else {
//                System.out.println("Unknown record schema");
//            }
//        } // for record
        return zeilen;
    }

    private static ArrayList<Row> dataViews2Rows(List<DataView> dataviews) {
        ArrayList arrayOfRows = new ArrayList<Row>();
        for (DataView dataview : dataviews) {
            if (dataview.isMimeType(KWICDataView.MIMETYPE)) {
                KWICDataView kw = (KWICDataView) dataview;

                Row row = new Row();

                Label toTheLeft = new Label();
                toTheLeft.setMultiline(true);
                toTheLeft.setValue(kw.getLeft());
                toTheLeft.setSclass("word-wrap");

                row.appendChild(toTheLeft);
                Label l = new Label(kw.getKeyword());
                l.setStyle("color:#8f3337;");
                l.setMultiline(true);
                l.setSclass("word-wrap");
                row.appendChild(l);

                Label toTheRight = new Label();
                toTheRight.setMultiline(true);
                toTheRight.setSclass("word-wrap");
                toTheRight.setValue(kw.getRight());

                row.appendChild(toTheRight);

                arrayOfRows.add(row);

            }
        } // for
        return arrayOfRows;
    }
}
