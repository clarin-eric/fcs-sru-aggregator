package clarind.fcs;

import java.util.ArrayList;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Row;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Button;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Separator;
import org.zkoss.zul.Label;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Column;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Window;
import java.util.logging.*;


public class Aggregator extends SelectorComposer<Component> {
    
    private static Logger logger = Logger.getLogger("FCS-AGGREGATOR");

    @Wire
    private Grid anzeigeGrid;
    @Wire
    private Textbox searchString;
    @Wire
    private Combobox languageSelect;
    @Wire
    private Button searchButton;
    @Wire
    private Checkbox ids1;
    @Wire
    private Groupbox allCorpora;
    @Wire
    private Comboitem german;
    @Wire
    private Vbox resultsVbox;
    @Wire
    private Button selectAll;
    @Wire
    private Button deselectAll;
    @Wire
    private Window mainWindow;
    @Wire
    private Combobox maximumRecordsSelect;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp); 

        languageSelect.setSelectedItem(german);

        Harvester harv = new Harvester();
        ArrayList<Endpoint> ep = harv.getEndpoints();

        int i, i2;

        for (i = 0; i < ep.size(); i++) {

            logger.info("Calling corpora ...: " + ep.get(i).getUrl());
            ArrayList<Corpus> corpora = harv.getCorporaOfAnEndpoint(ep.get(i).getUrl());

            if (corpora.isEmpty()) {
                Checkbox cb = new Checkbox();
                cb.setId(ep.get(i).getUrl());

                //"?operation=searchRetrieve&version=1.2"
                cb.setLabel(ep.get(i).getUrl());

                allCorpora.getChildren().add(cb);
                allCorpora.getChildren().add(new Separator());

                logger.info("Created Checkbox for endpoint" + cb.getId());
            } else {
                Label l = new Label(ep.get(i).getUrl() + ":");

                l.setStyle("font-weight:bold");

                allCorpora.getChildren().add(l);
                allCorpora.getChildren().add(new Separator());
                for (i2 = 0; i2 < corpora.size(); i2++) {
                    Checkbox cb = new Checkbox();

                    cb.setId(ep.get(i).getUrl() + "\t" + corpora.get(i2).getValue());
                    cb.setLabel(corpora.get(i2).getDisplayTerm());

                    allCorpora.getChildren().add(cb);
                    allCorpora.getChildren().add(new Separator());

                    logger.info("Created Checkbox for corpus " + cb.getId());
                } // for i2 ...
            } // if corpora.size else

            Separator sep = new Separator();

            sep.setBar(true);
            allCorpora.getChildren().add(sep);

        } // for i ...

    }

    @Listen("onClick = #selectAll")
    public void onSelectAll(Event ev) {
        int i;

        for (i = 0; i < allCorpora.getChildren().size(); i++) {
            if (allCorpora.getChildren().get(i) instanceof Checkbox) {
                Checkbox cb = (Checkbox) allCorpora.getChildren().get(i);
                cb.setChecked(true);
            }
        }
    } //onSelectAll

    @Listen("onClick = #deselectAll")
    public void onDeselectAll(Event ev) {
        int i;

        for (i = 0; i < allCorpora.getChildren().size(); i++) {
            if (allCorpora.getChildren().get(i) instanceof Checkbox) {
                Checkbox cb = (Checkbox) allCorpora.getChildren().get(i);
                cb.setChecked(false);
            }
        }
    } //onDeselectAll

    @Listen("onSelect = #languageSelect")
    public void onSelectLanguage(Event ev) {
        try {
            ids1.setDisabled(true);
        } catch (Exception ex) {
        }
    }

    @Listen("onClick=#clearResults")
    public void onClearResults(Event ev) {
        resultsVbox.getChildren().clear();
    }

    @Listen("onClick=#showHelp")
    public void onShowHelp(Event ev) {
        resultsVbox.getChildren().clear();
        Iframe help = new Iframe();
        help.setWidth("100%");
        help.setHeight("100%");
        help.setSrc("help.html");
        resultsVbox.appendChild(help);
    }

    @Listen("onClick=#showAbout")
    public void onShowAbout(Event ev) {
        Messagebox.show("CLARIN-D Federated Content Search Aggregator\n\nVersion 0.0.1", "FCS", 0, Messagebox.INFORMATION);

    }

    @Listen("onClick=#exportResultsCSV")
    public void onExportResultsCSV(Event ev) {

        int i, i2, i3;
        String temp = "";
        boolean somethingToExport = false;

        for (i = 0; i < resultsVbox.getChildren().size(); i++) {
            if (resultsVbox.getChildren().get(i) instanceof Grid) {
                somethingToExport = true;
                Grid aGrid = (Grid) resultsVbox.getChildren().get(i);
                Rows rows = aGrid.getRows();

                for (i2 = 0; i2 < rows.getChildren().size(); i2++) {
                    Row r = (Row) rows.getChildren().get(i2);

                    for (i3 = 0; i3 < r.getChildren().size(); i3++) {
                        Label l = (Label) r.getChildren().get(i3);
                        temp = temp + "\"" + l.getValue().replace("\"", "QUOTE")  + "\"";
                        if (i3 < r.getChildren().size() - 1) {
                            temp = temp + ",";
                        } //if i3
                    } //for i3
                    temp = temp + "\n";
                } // for i2
            } // if grid

        } // for i ...

        if (somethingToExport) {

            Filedownload.save(temp, "text/plain", "ClarinDFederatedContentSearch.csv");
        } else {
            Messagebox.show("Nothing to export!");
        }
    }

    @Listen("onClick=#exportResultsTCF")
    public void onExportResultsTCF(Event ev) {

        int i, i2, i3;
        String temp = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><D-Spin xmlns=\"http://www.dspin.de/data\" version=\"0.4\">\n<MetaData xmlns=\"http://www.dspin.de/data/metadata\">\n";
        temp = temp + "<source>CLARIN-D Federated Content Search</source>\n</MetaData>\n  <TextCorpus xmlns=\"http://www.dspin.de/data/textcorpus\">\n<text>";


        boolean somethingToExport = false;

        for (i = 0; i < resultsVbox.getChildren().size(); i++) {
            if (resultsVbox.getChildren().get(i) instanceof Grid) {
                somethingToExport = true;
                Grid aGrid = (Grid) resultsVbox.getChildren().get(i);
                Rows rows = aGrid.getRows();

                for (i2 = 0; i2 < rows.getChildren().size(); i2++) {
                    Row r = (Row) rows.getChildren().get(i2);

                    for (i3 = 0; i3 < r.getChildren().size(); i3++) {
                        Label l = (Label) r.getChildren().get(i3);
                        temp = temp + l.getValue() + " ";
                    } //for i3
                    temp = temp + "\n";
                } // for i2
            } // if grid

        } // for i ...

        if (somethingToExport) {
            temp = temp + "</text>\n</TextCorpus>\n</D-Spin>";
            Filedownload.save(temp, "text/tcf+xml", "ClarinDFederatedContentSearch.xml");
        } else {
            Messagebox.show("Nothing to export!");
        }
    }

    @Listen("onClick = #searchButton")
    public void onExecuteSearch(Event ev) {

        try {

            if (languageSelect.getText().trim().equals("")) {
                Messagebox.show("Please select a language.");
                return;
            }

            int i, i2;

            resultsVbox.getChildren().clear();

            boolean isACorpusSelected = false;
                     
            //SRUSearch srusearch = new SRUSearch();
                        
            SRUSearchThreaded srusearch =  SRUSearchThreaded.getInstance();

            for (i = 0; i < allCorpora.getChildren().size(); i++) {
                if (allCorpora.getChildren().get(i) instanceof Checkbox) {
                    Checkbox cb = (Checkbox) allCorpora.getChildren().get(i);
                    if (cb.isChecked()) {
                        // now execute the search:

                        isACorpusSelected = true;
                        
                        String endpointURL = null;
                        String corpus = null;

                        if (cb.getId().contains("\t")) {
                            endpointURL = cb.getId().split("\t")[0];
                            corpus = cb.getId().split("\t")[1];
                        } else {
                            endpointURL = cb.getId();
                        }

                        resultsVbox.appendChild(new Label("Query: " + searchString.getText()));
                        resultsVbox.appendChild(new Label("Endpoint: " + endpointURL));
                        if (corpus != null) {
                            resultsVbox.appendChild(new Label("Corpus: " + corpus));
                        }

                        int maximumRecords = Integer.parseInt(maximumRecordsSelect.getValue());
                        
                        if(maximumRecords > 30){
                            Messagebox.show("The allowed maximum of hits is 30! Please don't specify a higher value!");
                            break;
                        }
                        
                        
                        logger.info("Now executing search: " + searchString.getText() + " " + endpointURL + " " +  corpus + " " + maximumRecords);
                      
                        ArrayList<Row> zeilen = new ArrayList<Row>();
                        
                        try {
                            zeilen = srusearch.execute(searchString.getText(), endpointURL, corpus, maximumRecords);
                        } catch (Exception ex) {
                            System.out.println(ex.getMessage());
                        }

                        if (zeilen.size() > 0) {

                            Grid g = new Grid();

                            g.setWidth("100%");
                            g.setMold("paging");
                            g.setPageSize(10);

                            Columns columns = new Columns();

                            Column c = new Column();
                            c.setLabel("Left");

                            columns.appendChild(c);

                            c = new Column();
                            c.setLabel("Hit");
                            c.setHflex("min");
                            columns.appendChild(c);

                            c = new Column();
                            c.setLabel("Right");
                            columns.appendChild(c);

                            g.appendChild(columns);

                            Rows rows = new Rows();

                            for (i2 = 0; i2 < zeilen.size(); i2++) {
                                rows.appendChild(zeilen.get(i2));
                            } // for i2 ...

                            g.appendChild(rows);

                            resultsVbox.appendChild(g);
                        } else {
                            resultsVbox.appendChild(new Label("Sorry there were no results!"));

                        } // if zeilen > 0

                        Separator sep = new Separator();
                        sep.setSpacing("20px");
                        resultsVbox.appendChild(sep);
                    }
                }
            } // for i ...
            
           
            
            
            if (!isACorpusSelected) {

                Messagebox.show("Please select at least one corpus!", "CLARIN-D FCS Aggregator", 0, Messagebox.EXCLAMATION);
            }


           logger.info("Search done.");

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

    }
}
