package clarind.fcs;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Button;
import org.zkoss.zul.Messagebox;

public class Aggregator extends GenericAutowireComposer {

    private Button button1;
    private Textbox textbox;

    public void onPressButton(Event ev) {
        try {
            Messagebox.show("Hallo Welt! Der Wert der Textbox wird bei jedem Klick um 1 erhöht ...");
            
            textbox.setText(Integer.toString(Integer.parseInt(textbox.getText()) + 1));
            
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

    }
}
