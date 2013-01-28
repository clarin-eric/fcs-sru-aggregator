package eu.clarin.sru.fcs.aggregator.sresult;

import eu.clarin.sru.client.SRURecord;
import eu.clarin.sru.client.SRUSurrogateRecordData;
import eu.clarin.sru.client.fcs.ClarinFCSRecordData;
import eu.clarin.sru.client.fcs.DataView;
import eu.clarin.sru.client.fcs.DataViewKWIC;
import eu.clarin.sru.client.fcs.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;

/**
 * Renders SRURecord data to the specified row.
 *
 * @author Yana Panchenko
 */
public class SearchResultRecordRenderer implements RowRenderer {

    @Override
    public void render(Row row, Object data, int index) throws Exception {

        SRURecord record = (SRURecord) data;

        Logger.getLogger(this.getClass().getName()).info(
                String.format("schema = %s, identifier = %s, position = %s",
                new Object[]{record.getRecordSchema(),
                    record.getRecordIdentifier(),
                    record.getRecordPosition()}));

        if (record.isRecordSchema(ClarinFCSRecordData.RECORD_SCHEMA)) {
            ClarinFCSRecordData rd =
                    (ClarinFCSRecordData) record.getRecordData();




            Resource resource = rd.getResource();


//            if (resource.getRef() != null) {
//                //resourceNames.add(resource.getRef());
//                row.appendChild(new Label(resource.getRef()));
//
//            } else if (resource.getPid() != null) {
//                //resourceNames.add(resource.getPid());
//                row.appendChild(new Label(resource.getPid()));
//            } else {
//                row.appendChild(new Label(" "));
//            }


            // If dataviews are assigned directly to the resource:                    
            if (resource.hasDataViews()) {
                //zeilen.addAll(dataViews2Rows(resource.getDataViews()));
                appendDataView(row, resource.getDataViews());
            }

            // If there are resource fragments:
            if (resource.hasResourceFragments()) {
                for (Resource.ResourceFragment fragment : resource.getResourceFragments()) {
                    Logger.getLogger(this.getClass().getName()).info(
                            String.format("CLARIN-FCS: ResourceFragment: pid=%s, ref=%s",
                            fragment.getPid(), fragment.getRef()));
                    if (fragment.hasDataViews()) {
                        //zeilen.addAll(dataViews2Rows(fragment.getDataViews()));
                        appendDataView(row, fragment.getDataViews());
                    }
                }
            } //ResourceFragments

        } else if (record.isRecordSchema(SRUSurrogateRecordData.RECORD_SCHEMA)) {
            SRUSurrogateRecordData r =
                    (SRUSurrogateRecordData) record.getRecordData();
            Logger.getLogger(this.getClass().getName()).info(
                    String.format("SURROGATE DIAGNOSTIC: uri=%s, message=%s, detail=%s",
                    r.getURI(), r.getMessage(), r.getDetails()));
        } else {
            Logger.getLogger(this.getClass().getName()).info(
                    String.format("UNSUPPORTED SCHEMA: %s",
                    record.getRecordSchema()));
        }

    }

    private void appendDataView(Row row, List<DataView> dataViews) {

        for (DataView dataview : dataViews) {

            // ***** Handling the KWIC dataviews
            if (dataview.isMimeType(DataViewKWIC.TYPE)) {
                DataViewKWIC kw = (DataViewKWIC) dataview;


                Label toTheLeft = new Label();
                toTheLeft.setMultiline(true);
                toTheLeft.setValue(kw.getLeft());
                toTheLeft.setSclass("word-wrap");
                Cell toTheLeftCell = new Cell();
                toTheLeftCell.appendChild(toTheLeft);
                toTheLeftCell.setAlign("right");
                toTheLeftCell.setValign("bottom");
                row.appendChild(toTheLeftCell);
                //row.appendChild(toTheLeft);
                
                
                Label l = new Label(kw.getKeyword());
                l.setStyle("color:#8f3337;");
                l.setMultiline(true);
                l.setSclass("word-wrap");
                Cell lCell = new Cell();
                lCell.appendChild(l);
                lCell.setAlign("center");
                lCell.setValign("bottom");
                row.appendChild(lCell);
                //row.appendChild(l);

                Label toTheRight = new Label();
                toTheRight.setMultiline(true);
                toTheRight.setSclass("word-wrap");
                toTheRight.setValue(kw.getRight());
                Cell toTheRightCell = new Cell();
                toTheRightCell.appendChild(toTheRight);
                toTheRightCell.setValign("bottom");
                row.appendChild(toTheRightCell);
                //row.appendChild(toTheRight);

            }
        }
    }
}