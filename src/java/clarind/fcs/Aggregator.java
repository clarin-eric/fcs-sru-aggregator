package clarind.fcs;

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
import org.zkoss.zul.Comboitem;

public class Aggregator extends SelectorComposer<Component> {

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
                 cb.setId(ep.get(i).getUrl() + "?operation=searchRetrieve&version=1.2");
                cb.setLabel(ep.get(i).getUrl());
                
                allCorpora.getChildren().add(cb);
                allCorpora.getChildren().add(new Separator());
            } else {
                Label l = new Label(ep.get(i).getUrl() + ":");

                allCorpora.getChildren().add(l);
                allCorpora.getChildren().add(new Separator());
                for (i2 = 0; i2 < corpora.size(); i2++) {
                    Checkbox cb = new Checkbox();
                    
                    //http://clarinws.informatik.uni-leipzig.de:8080/CQL?operation=searchRetrieve&version=1.2&query=Boppard&x-context=11858/00-229C-0000-0003-174F-D&maximumRecords=2

                    cb.setId(ep.get(i).getUrl() + "?operation=searchRetrieve&version=1.2&x-context=" + corpora.get(i2).getValue());
                    cb.setLabel(corpora.get(i2).getDisplayTerm());
                    
                    allCorpora.getChildren().add(cb);
                    allCorpora.getChildren().add(new Separator());
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
        try {
            
            if (languageSelect.getText().trim().equals("")){
                  Messagebox.show("Please select a language.");
                  return;
            }
            
            String display = "SearchString: " + searchString.getText() + "\n";

            display = display + "Language: " + languageSelect.getSelectedItem().getLabel() + "\n";

            int i, i2;

            // ----- IDS:

            display = display + "Corpora:\n";

            for (i = 0; i < allCorpora.getChildren().size(); i++) {
                if (allCorpora.getChildren().get(i) instanceof Checkbox) {
                    Checkbox cb = (Checkbox) allCorpora.getChildren().get(i);
                    if (cb.isChecked()) {
                        // now execute the search:
                        String query = cb.getId() + "&maximumRecords=10&query=" + searchString.getText();
                        display = display + query + "\n";
                    }
                }
            } // for i ...

            Messagebox.show(display);
            System.out.println(display);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

    }
}
