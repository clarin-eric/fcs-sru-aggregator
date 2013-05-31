/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.clarin.sru.fcs.aggregator.app;

import org.zkoss.zul.A;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Menubar;
import org.zkoss.zul.North;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.South;

/**
 *
 * @author yanapanchenko
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
     
     //private int selectedPage = 0;

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
        this.srDiv.setVisible(true);
        this.srLabel.setSclass(LINK_SELECTED_CLASS);
        this.aboutDiv.setVisible(false);
        this.aboutLabel.setSclass(LINK_CLASS);
        this.soDiv.setVisible(false);
        this.soLabel.setSclass(LINK_CLASS);
        this.helpDiv.setVisible(false);
        this.helpLabel.setSclass(LINK_CLASS);
        //this.selectedPage = 2;
    }
    
    public void openSearchOptions() {
    
        this.soDiv.setVisible(true);
        this.soLabel.setSclass(LINK_SELECTED_CLASS);
        this.aboutDiv.setVisible(false);
        this.aboutLabel.setSclass(LINK_CLASS);
        this.helpDiv.setVisible(false);
        this.helpLabel.setSclass(LINK_CLASS);
        this.srDiv.setVisible(false);
        this.srLabel.setSclass(LINK_CLASS);
        //this.selectedPage = 1;
    }

    void openAbout() {
        this.aboutDiv.setVisible(true);
        this.aboutLabel.setSclass(LINK_SELECTED_CLASS);
        this.helpDiv.setVisible(false);
        this.helpLabel.setSclass(LINK_CLASS);
        this.soDiv.setVisible(false);
        this.soLabel.setSclass(LINK_CLASS);
        this.srDiv.setVisible(false);
        this.srLabel.setSclass(LINK_CLASS);
        //this.selectedPage = 0;
    }

    void openHelp() {
        this.helpDiv.setVisible(true);
        this.helpLabel.setSclass(LINK_SELECTED_CLASS);
        this.aboutDiv.setVisible(false);
        this.aboutLabel.setSclass(LINK_CLASS);
        this.soDiv.setVisible(false);
        this.soLabel.setSclass(LINK_CLASS);
        this.srDiv.setVisible(false);
        this.srLabel.setSclass(LINK_CLASS);
        //this.selectedPage = 3;
    }

    //
    //    public boolean hasRearchResultOpen() {
    //    }
    //    }
    @Override
    public String toString() {
        return "PagesVisibility{" + "aboutDiv=" + aboutDiv + ", aboutLabel=" + aboutLabel + ", soDiv=" + soDiv + ", soLabel=" + soLabel + ", srDiv=" + srDiv + ", srLabel=" + srLabel + ", helpDiv=" + helpDiv + ", helpLabel=" + helpLabel + '}';
    }
    
    
}
