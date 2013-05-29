package eu.clarin.sru.fcs.aggregator.sparam2;

import eu.clarin.sru.client.SRURecord;
import eu.clarin.sru.client.SRUSurrogateRecordData;
import eu.clarin.sru.client.fcs.ClarinFCSRecordData;
import eu.clarin.sru.client.fcs.DataView;
import eu.clarin.sru.client.fcs.DataViewKWIC;
import eu.clarin.sru.client.fcs.Resource;
import eu.clarin.sru.fcs.aggregator.sparam2.SearchResult2;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.A;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.DefaultTreeNode;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Popup;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Treerow;
import org.zkoss.zul.Vlayout;
import org.zkoss.zul.Window;

/**
 * Renders SRURecord data to the specified row.
 *
 * @author Yana Panchenko
 */
public class SearchResultRecordRenderer2 implements RowRenderer {

    private SearchResult2 searchResult;
    private static final Logger logger = Logger.getLogger(SearchResultRecordRenderer2.class.getName());

    public SearchResultRecordRenderer2(SearchResult2 searchResult) {
        this.searchResult = searchResult;
    }

    @Override
    public void render(Row row, Object data, int index) throws Exception {

        SRURecord record = (SRURecord) data;

        logger.log(Level.FINE,
                "schema = {0}, identifier = {1}, position = {2}",
                new Object[]{record.getRecordSchema(),
                    record.getRecordIdentifier(),
                    record.getRecordPosition()});

        if (record.isRecordSchema(ClarinFCSRecordData.RECORD_SCHEMA)) {
            ClarinFCSRecordData rd =
                    (ClarinFCSRecordData) record.getRecordData();

            Resource resource = rd.getResource();

            logger.log(Level.FINE,
                    "Resource ref={0}, pid={1}, dataViews={2}",
                    new Object[]{resource.getRef(), resource.getPid(), resource.hasDataViews()});




            String pid = resource.getPid();
            String reference = resource.getRef();
            // If dataviews are assigned directly to the resource:                    
            if (resource.hasDataViews()) {
                appendDataView(row, resource.getDataViews(), resource.getPid(), resource.getRef());
                appendResourceInfo(row, pid, reference);
            }

            // If there are resource fragments:
            if (resource.hasResourceFragments()) {
                for (Resource.ResourceFragment fragment : resource.getResourceFragments()) {
                    logger.log(Level.FINE, "ResourceFragment: ref={0}, pid={1}, dataViews={2}",
                            new Object[]{fragment.getRef(), fragment.getPid(), fragment.hasDataViews()});
                    if (fragment.hasDataViews()) {
                        appendDataView(row, fragment.getDataViews(), fragment.getPid(), fragment.getRef());
                        if (fragment.getPid() != null) {
                            pid = fragment.getPid();
                        }
                        if (fragment.getRef() != null) {
                            reference = fragment.getRef();
                        }
                        appendResourceInfo(row, pid, reference);
                    }
                }
            }

            

        } else if (record.isRecordSchema(SRUSurrogateRecordData.RECORD_SCHEMA)) {
            SRUSurrogateRecordData r =
                    (SRUSurrogateRecordData) record.getRecordData();
            logger.log(Level.INFO, "Surrogate diagnostic: uri={0}, message={1}, detail={2}",
                    new Object[]{r.getURI(), r.getMessage(), r.getDetails()});
        } else {
            logger.log(Level.INFO, "Unsupported schema: {0}", record.getRecordSchema());
        }

    }

    private void appendDataView(Row row, List<DataView> dataViews, String pid, String reference) {

        for (DataView dataview : dataViews) {

            // ***** Handling the KWIC dataviews
            if (dataview.isMimeType(DataViewKWIC.TYPE)) {
                DataViewKWIC kw = (DataViewKWIC) dataview;
                this.searchResult.addKwic(kw, pid, reference);

                Label toTheLeft = new Label();
                toTheLeft.setValue(kw.getLeft());

                toTheLeft.setMultiline(true);
                toTheLeft.setSclass("word-wrap");
                Cell toTheLeftCell = new Cell();
                toTheLeftCell.appendChild(toTheLeft);
                toTheLeftCell.setAlign("right");
                toTheLeftCell.setValign("bottom");
                row.appendChild(toTheLeftCell);
//                row.appendChild(toTheLeft);


                Label l = new Label(kw.getKeyword());
                l.setStyle("color:#8f3337;");
                l.setMultiline(true);
                l.setSclass("word-wrap");
                Cell lCell = new Cell();
                lCell.appendChild(l);
                lCell.setAlign("center");
                lCell.setValign("bottom");
                row.appendChild(lCell);
//                row.appendChild(l);

                Label toTheRight = new Label();
                toTheRight.setValue(kw.getRight());
                toTheRight.setMultiline(true);
                toTheRight.setSclass("word-wrap");
                Cell toTheRightCell = new Cell();
                toTheRightCell.appendChild(toTheRight);
                toTheRightCell.setValign("bottom");
                row.appendChild(toTheRightCell);
//                row.appendChild(toTheRight);

            }
        }
    }

    private void appendResourceInfo(final Row row, String pid, String reference) {
        final Cell infoCell = new Cell();
        boolean hasInfo = false;
        final Window infoWin = new Window();
        infoWin.setTitle("Source");
        infoWin.setClosable(true);
        //infoWin.setWidth("300px");
        Hlayout hlayout = new Hlayout();
        hlayout.setParent(infoWin);
        Vlayout col1 = new Vlayout();
        col1.setStyle("margin:10px;");
        hlayout.appendChild(col1);
        Vlayout col2 = new Vlayout();
        hlayout.appendChild(col2);
        col2.setStyle("margin:10px;");
        if (reference != null) {
            col1.appendChild(new Label("Reference: "));
            A link = new A(reference);
            link.setTarget("_blank");
            link.setHref(reference);
            col2.appendChild(link);
            hasInfo = true;
        } 
        if (pid != null) {
            col1.appendChild(new Label("PID: "));
            col2.appendChild(new Label(pid));
            hasInfo = true;
        }

        if (hasInfo) {
            Image infoImage = new Image("help-about.png");
            infoImage.setStyle("margin-right:10px;");
            infoCell.appendChild(infoImage);
            infoCell.addEventListener(Events.ON_CLICK, new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    infoWin.setParent(infoCell);
                    infoWin.doModal();
                    infoWin.setPosition("right,center");
                }
            });



        } else {
            Label label = new Label("");
            label.setParent(infoCell);
        }
        //infoCell.setStyle("margin-right:10px;");
        row.appendChild(infoCell);
    }
    
//    private void appendEmptyCell(Row row) {
//        Cell cell = new Cell();
//        Label label = new Label("");
//        label.setParent(cell);
//        row.appendChild(cell);
//    }
}