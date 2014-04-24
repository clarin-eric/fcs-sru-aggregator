package eu.clarin.sru.fcs.aggregator.sresult;

import eu.clarin.sru.client.SRURecord;
import eu.clarin.sru.fcs.aggregator.util.ZKComp;
import java.util.List;
import java.util.logging.Logger;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.A;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vlayout;
import org.zkoss.zul.Window;

/**
 * Creates Groupbox component containing SearchResult records from an endpoint 
 * resource, its caption containing information about the endpoint resource of 
 * the corresponding SearchResult.
 * 
 * @author Yana Panchenko
 */
public class SearchResultGroupRenderer {
    
   public static final String NO_RESULTS = "nr";
   private static final Logger LOGGER = Logger.getLogger(SearchResultGroupRenderer.class.getName());
    

    public Groupbox createRecordsGroup(SearchResult resultsItem) {

        Groupbox recordsGroup = new Groupbox();
        recordsGroup.setMold("3d");
        recordsGroup.setSclass("ccsLightBlue");
        recordsGroup.setContentStyle("border:0;");
        recordsGroup.setStyle("margin:10px;10px;10px;10px;");
        recordsGroup.setClosable(true);
        String title = createRecordsGoupTitle(resultsItem);
        recordsGroup.setTitle(title);
        Caption caption = createRecordsGroupCaption(resultsItem);
        recordsGroup.appendChild(caption);

        // populate it with records grid or failure message
        if (resultsItem.getResponse() == null) { // there was an error in response
            recordsGroup.appendChild(new Label("Sorry, the search failed!"));
            recordsGroup.setAttribute(NO_RESULTS, true);
        } else if (resultsItem.getResponse().hasRecords()) { // the response was fine and there >=1 records
            Grid grid = new Grid();
//            grid.setWidth("100%");
//            grid.setMold("paging");
//            grid.setPageSize(10);
            Columns columns = new Columns();
            Column c;

            c = new Column();
            //c.setLabel("Left");
            columns.appendChild(c);
            //c.setHflex("2");
            c = new Column();
            //c.setLabel("Hit");
            c.setHflex("min");
            //c.setHflex("1");
            columns.appendChild(c);
            c = new Column();
            //c.setHflex("2");
            //c.setLabel("Right");
            // info column
            columns.appendChild(c);
            c = new Column();
            c.setHflex("min");
            columns.appendChild(c);
            grid.appendChild(columns);

            List<SRURecord> sruRecords = resultsItem.getResponse().getRecords();
            ListModel lmodel = new SimpleListModel(sruRecords);
            grid.setModel(lmodel);
            grid.setRowRenderer(new SearchResultRecordRenderer(resultsItem));
            recordsGroup.appendChild(grid);
            grid.setStyle("margin:10px;border:0px;");
        } else { // the response was fine, but there are no records
            recordsGroup.appendChild(new Label("no results"));
            recordsGroup.setAttribute(NO_RESULTS, true);
        }
        return recordsGroup;
    }
    
    
    
    private Caption createRecordsGroupCaption(SearchResult resultsItem) {
//        StringBuilder sb = new StringBuilder();
//        if (resultsItem.hasCorpusHandler()) {
//            if (resultsItem.getCorpus().getDisplayName() != null) {
//                sb.append(resultsItem.getCorpus().getDisplayName());
//                sb.append(", ");
//            }
//            if (!resultsItem.getCorpus().getDescription().isEmpty()) {
//            }
//        }
//        sb.append(resultsItem.getCorpus().getInstitution().getName());
//        Caption caption = new Caption(sb.toString());
        
        
        final Caption caption = new Caption();

        A homeLink = null;
        Label recordsFound = null;
        final Toolbarbutton infoCell = new Toolbarbutton();
        
        if (resultsItem.getCorpus().getLandingPage() != null) {
            homeLink = ZKComp.createCorpusHomeLink(resultsItem.getCorpus().getLandingPage());
            homeLink.setStyle("margin-left:10px;margin-right:10px;");
        }
        
        final Window infoWin = new Window();
        infoWin.setTitle("Resource information");
        infoWin.setClosable(true);
        
        infoWin.setWidth("40%");
        Vlayout vlayout = new Vlayout();
        vlayout.setParent(infoWin);
        Hlayout pidInfo = new Hlayout();
        pidInfo.setStyle("margin:10px;");
        vlayout.appendChild(pidInfo);
        Hlayout recordsInfo = new Hlayout();
        recordsInfo.setStyle("margin:10px;");
        vlayout.appendChild(recordsInfo);
        Hlayout description = new Hlayout();
        vlayout.appendChild(description);
        description.setStyle("margin:10px;");
        boolean hasInfo = false;
        if (resultsItem.getCorpus().getHandle() != null) {
            pidInfo.appendChild(new Label("PID: "));
            pidInfo.appendChild(new Label(resultsItem.getCorpus().getHandle()));
            hasInfo = true;
        }
        if (resultsItem.getCorpus().getNumberOfRecords() != null) {
            recordsInfo.appendChild(new Label("SIZE: "));
            recordsInfo.appendChild(new Label(resultsItem.getCorpus().getNumberOfRecords().toString() + " records"));
        }
        if (resultsItem.getCorpus().getDescription() != null) {
            description.appendChild(new Label(resultsItem.getCorpus().getDescription()));
            hasInfo = true;
        }
        
        //TODO file the bug to Oliver - always returns -1 even if there is number of records provided
        //LOGGER.info(resultsItem.getCorpus().getDisplayName() + " R: " + resultsItem.getResponse().getNumberOfRecords());
        StringBuilder sb2 = new StringBuilder("hits " + 
                resultsItem.getStartRecord() + "-" + (resultsItem.getEndRecord()));
        
        if (resultsItem.getResponse() != null && resultsItem.getResponse().getNumberOfRecords() > 0) {
            sb2.append(" from " + resultsItem.getResponse().getNumberOfRecords() + " found");
        }
        recordsFound = new Label(sb2.toString());
        recordsFound.setStyle("margin-left:10px;margin-right:30px;");
        if (hasInfo) {
            Image infoImage = new Image("img/help-about.png");
            infoImage.setStyle("margin-right:10px;");
            infoCell.setImage("img/help-about.png");
            infoCell.addEventListener(Events.ON_CLICK, new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    infoWin.setParent(caption);
                    infoWin.doModal();
                    infoWin.setPosition("center,center");
                }
            });

        } else {
            infoCell.setLabel("");
        }
        caption.appendChild(recordsFound);
        if (homeLink != null) {
            caption.appendChild(homeLink);
        }
        caption.appendChild(infoCell);
        
        return caption;
    }

    private String createRecordsGoupTitle(SearchResult resultsItem) {
        StringBuilder sb = new StringBuilder();
        if (resultsItem.hasCorpusHandler()) {
            if (resultsItem.getCorpus().getDisplayName() != null) {
                sb.append(resultsItem.getCorpus().getDisplayName());
                sb.append(", ");
            }
        }
        sb.append(resultsItem.getCorpus().getInstitution().getName());
        return sb.toString();
    }
    
}
