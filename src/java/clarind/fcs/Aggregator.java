package clarind.fcs;

import eu.clarin.sru.client.SRUClient;
import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUDefaultHandlerAdapter;
import eu.clarin.sru.client.SRURecordData;
import eu.clarin.sru.client.SRUSearchRetrieveRequest;
import eu.clarin.sru.client.SRUVersion;
import eu.clarin.sru.fcs.ClarinFederatedContentSearchRecordData;
import eu.clarin.sru.fcs.ClarinFederatedContentSearchRecordParser;
import java.util.ArrayList;
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
import org.zkoss.zul.Html;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Grid;

public class Aggregator extends SelectorComposer<Component> {

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
    private Html anzeigeResults;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp); //wire variables and event listners
        //do whatever you want (you could access wired variables here)

        languageSelect.setSelectedItem(german);

        Harvester harv = new Harvester();
        ArrayList<Endpoint> ep = harv.getEndpoints();

        int i, i2;

        for (i = 0; i < ep.size(); i++) {

            System.out.println("Calling corpora ...: " + ep.get(i).getUrl());
            ArrayList<Corpus> corpora = harv.getCorporaOfAnEndpoint(ep.get(i).getUrl());

            if (corpora.size() == 0) {
                Checkbox cb = new Checkbox();
                cb.setId(ep.get(i).getUrl());

                //"?operation=searchRetrieve&version=1.2"
                cb.setLabel(ep.get(i).getUrl());

                allCorpora.getChildren().add(cb);
                allCorpora.getChildren().add(new Separator());

                System.out.println("CHECKBOX: " + cb.getId());
            } else {
                Label l = new Label(ep.get(i).getUrl() + ":");

                allCorpora.getChildren().add(l);
                allCorpora.getChildren().add(new Separator());
                for (i2 = 0; i2 < corpora.size(); i2++) {
                    Checkbox cb = new Checkbox();

                    //http://clarinws.informatik.uni-leipzig.de:8080/CQL?operation=searchRetrieve&version=1.2&query=Boppard&x-context=11858/00-229C-0000-0003-174F-D&maximumRecords=2

                    cb.setId(ep.get(i).getUrl() + "\t" + corpora.get(i2).getValue());
                    cb.setLabel(corpora.get(i2).getDisplayTerm());

                    allCorpora.getChildren().add(cb);
                    allCorpora.getChildren().add(new Separator());

                    System.out.println("CHECKBOX: " + cb.getId());
                } // for i2 ...
            } // if corpora.size else
        } // for i ...
    }

    @Listen("onSelect = #languageSelect")
    public void onSelectLanguage(Event ev) {
        try {
            ids1.setDisabled(true);
        } catch (Exception ex) {
        }
    }

    @Listen("onClick = #searchButton")
    public void onExecuteSearch(Event ev) {
//        try {
//            ExecuteSRUSearch.execute();
//        } catch (Exception ex){
//            System.out.println(ex.getMessage());
//        }



        try {

            if (languageSelect.getText().trim().equals("")) {
                Messagebox.show("Please select a language.");
                return;
            }

            anzeigeResults.setContent("");
            
            int i, i2;

            for (i = 0; i < allCorpora.getChildren().size(); i++) {
                if (allCorpora.getChildren().get(i) instanceof Checkbox) {
                    Checkbox cb = (Checkbox) allCorpora.getChildren().get(i);
                    if (cb.isChecked()) {
                        // now execute the search:
//                        String query = cb.getId() + "&maximumRecords=10&query=" + searchString.getText();
//                        display = display + query + "\n";
                        System.out.println("---- THE SEARCH ----");

                        String endpointURL = null;
                        String corpus = null;

                        if (cb.getId().contains("\t")) {
                            endpointURL = cb.getId().split("\t")[0];
                            corpus = cb.getId().split("\t")[1];
                        } else {
                            endpointURL = cb.getId();
                        }

                        System.out.println("enddpointURL: " + endpointURL);
                        System.out.println("corpus: " + corpus);
                        SRUSearch srusearch = new SRUSearch();
                        anzeigeResults.setContent(anzeigeResults.getContent() + srusearch.execute(searchString.getText(), endpointURL, corpus, 10, anzeigeResults, anzeigeGrid).toString());



                    }
                }
            } // for i ...
            System.out.println("Done");
            //Messagebox.show(display);

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

    }
}
