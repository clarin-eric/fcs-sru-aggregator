package clarind.fcs;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Row;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;

public class Aggregator extends GenericAutowireComposer {

    private Textbox searchString;
    private Rows rowsIds;
    private Rows rowsTue;
    private Combobox languageSelect;

    public void onExecuteSearch(Event ev) {
        try {
            String display = "SearchString: " + searchString.getText() + "\n";
            
            display = display + "Language: " + languageSelect.getSelectedItem().getLabel() + "\n";

            int i, i2;

            // ----- IDS:

            display = display + "IDS:\n";

            for (i = 0; i < rowsIds.getChildren().size(); i++) {
                Row r = (Row) rowsIds.getChildren().get(i);

                for (i2 = 0; i2 < r.getChildren().size(); i2++) {
                    Checkbox cb = (Checkbox) r.getChildren().get(i2);
                    if (cb.isChecked()) {
                        display = display + cb.getLabel() + "\n";
                    }
                } // for i2...

            } // for i ...


            // ----- Tübingen: 

            display = display + "Tübingen:\n";

            for (i = 0; i < rowsTue.getChildren().size(); i++) {
                Row r = (Row) rowsTue.getChildren().get(i);

                for (i2 = 0; i2 < r.getChildren().size(); i2++) {
                    Checkbox cb = (Checkbox) r.getChildren().get(i2);
                    if (cb.isChecked()) {
                        display = display + cb.getLabel() + "\n";
                    }
                } // for i2...

            } // for i ...

            Messagebox.show(display);

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

    }
}
