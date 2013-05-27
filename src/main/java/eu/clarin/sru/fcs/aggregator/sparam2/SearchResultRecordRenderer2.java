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





            // If dataviews are assigned directly to the resource:                    
            if (resource.hasDataViews()) {
                appendDataView(row, resource.getDataViews());
            }

            // If there are resource fragments:
            if (resource.hasResourceFragments()) {
                for (Resource.ResourceFragment fragment : resource.getResourceFragments()) {
                    logger.log(Level.FINE, "ResourceFragment: ref={0}, pid={1}, dataViews={2}",
                            new Object[]{fragment.getRef(), fragment.getPid(), fragment.hasDataViews()});
                    if (fragment.hasDataViews()) {
                        appendDataView(row, fragment.getDataViews());
                    }
                }
            }

            appendResourceInfo(row, resource);

        } else if (record.isRecordSchema(SRUSurrogateRecordData.RECORD_SCHEMA)) {
            SRUSurrogateRecordData r =
                    (SRUSurrogateRecordData) record.getRecordData();
            logger.log(Level.INFO, "Surrogate diagnostic: uri={0}, message={1}, detail={2}",
                    new Object[]{r.getURI(), r.getMessage(), r.getDetails()});
        } else {
            logger.log(Level.INFO, "Unsupported schema: {0}", record.getRecordSchema());
        }

    }

    private void appendDataView(Row row, List<DataView> dataViews) {

        for (DataView dataview : dataViews) {

            // ***** Handling the KWIC dataviews
            if (dataview.isMimeType(DataViewKWIC.TYPE)) {
                DataViewKWIC kw = (DataViewKWIC) dataview;
                this.searchResult.addKWIC(kw);

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

    private void appendResourceInfo(final Row row, Resource resource) {
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
        if (resource.getRef() != null) {
            //resourceNames.add(resource.getRef());
            //row.appendChild(new Label(resource.getRef()));
            col1.appendChild(new Label("Reference: "));
            A link = new A(resource.getRef());
            link.setTarget("_blank");
            link.setHref(resource.getRef());
            col2.appendChild(link);
            hasInfo = true;
        } else if (resource.getPid() != null) {
            //resourceNames.add(resource.getPid());
            col1.appendChild(new Label("PID: "));
            col2.appendChild(new Label(resource.getPid()));
            hasInfo = true;
        }

        if (hasInfo) {
            infoCell.appendChild(new Image("help-about.png"));
            infoCell.addEventListener(Events.ON_CLICK, new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    infoWin.setParent(infoCell);
                    infoWin.doModal();//.doOverlapped();
                    infoWin.setPosition("right");
                    //infoWin.setMode(Window.Mode.OVERLAPPED);//open(infoCell, "overlap_end/top_right");
                    //popup.appendChild(infoWin);
                }
            });



        } else {
            Label label = new Label("");
            label.setParent(infoCell);
        }
        row.appendChild(infoCell);
    }
}