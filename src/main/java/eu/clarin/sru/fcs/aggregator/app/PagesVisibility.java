package eu.clarin.sru.fcs.aggregator.app;

import org.zkoss.zul.Div;
import org.zkoss.zul.Label;

/**
 * Class to control which/how page is to be displayed in the main Aggregator window 
 * ('about' page, 'search options' page, 'results' page or 'help' page).
 * 
 * @author Yana Panchenko
 */
class PagesVisibility {
    
    private Div aboutDiv;
    private Label aboutLabel;
    
    private Div soDiv;
    private Label soLabel;
    
    private Div srDiv;
    private Label srLabel;
    
     private Div helpDiv;
     private Label helpLabel;
                
     private static final String LINK_CLASS = "internalLink";
     private static final String LINK_SELECTED_CLASS = "internalLinkSelected";
     

    public PagesVisibility(Div aboutDiv, Label aboutLabel, Div soDiv, Label soLabel,
            Div srDiv, Label srLabel, Div helpDiv, Label helpLabel) {
        this.aboutDiv = aboutDiv;
        this.aboutLabel = aboutLabel;
        this.soDiv = soDiv;
        this.soLabel = soLabel;
        this.srDiv = srDiv;
        this.srLabel = srLabel;
        this.helpDiv = helpDiv;
        this.helpLabel = helpLabel;
    }
    

    public void openSearchResult() {
        openPage(false, false, true, false);
    }
    
    public void openSearchOptions() {
        openPage(false, true, false, false);
    }

    void openAbout() {
        openPage(true, false, false, false);
    }

    public void openHelp() {
        openPage(false, false, false, true);
    }
    
    private void openPage(boolean about, boolean searchOptions, boolean searchResults, boolean help) {
        this.aboutDiv.setVisible(about);
        this.aboutLabel.setSclass(getSclass(about));
        this.soDiv.setVisible(searchOptions);
        this.soLabel.setSclass(getSclass(searchOptions));
        this.srDiv.setVisible(searchResults);
        this.srLabel.setSclass(getSclass(searchResults));
        this.helpDiv.setVisible(help);
        this.helpLabel.setSclass(getSclass(help));
    }
    
    
    private String getSclass(boolean open) {
        if (open) {
            return LINK_SELECTED_CLASS;
        } else {
            return LINK_CLASS;
        }
    }

    @Override
    public String toString() {
        return "PagesVisibility{" + "aboutDiv=" + aboutDiv + 
                ", aboutLabel=" + aboutLabel + ", soDiv=" + soDiv + ", "
                + "soLabel=" + soLabel + ", srDiv=" + srDiv + ", srLabel=" + srLabel
                + ", helpDiv=" + helpDiv + ", helpLabel=" + helpLabel + '}';
    }
    
    
}
