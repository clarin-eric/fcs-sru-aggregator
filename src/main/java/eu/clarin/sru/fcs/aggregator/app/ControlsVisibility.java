/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.clarin.sru.fcs.aggregator.app;

import org.zkoss.zul.A;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Menubar;
import org.zkoss.zul.North;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.South;

/**
 *
 * @author yanapanchenko
 */
class ControlsVisibility {
    private North controls1;
    private South controls2;
    private Progressmeter pMeter;
    private Menubar menubar;
    private A prev;
    private A next;
    
    private static final String CONTROLS1_SIZE = "30px";
    private static final String CONTROLS2_SIZE = "25px";
    private static final String CONTROLS_ZERO_SIZE = "0px";

    public ControlsVisibility(North controls1, South controls2, Progressmeter pMeter, Menubar menubar, A prev, A next) {
        this.controls1 = controls1;
        this.controls2 = controls2;
        this.pMeter = pMeter;
        this.menubar = menubar;
        this.prev = prev;
        this.next = next;
    }
    
//    public void turnOn() {
//        controls1.setSize(CONTROLS1_SIZE);
//        controls2.setSize(CONTROLS2_SIZE);
//        //menubar.setVisible(true);
//    }
//    
//    public void turnOff() {
//        disableControls1();
//        disableControls2();
//        //menubar.setVisible(false);
//    }
    
    public void enableProgressMeter(int value) {
        updateProgressMeter(value);
        pMeter.setVisible(true);
    }
    
    public void disableProgressMeter() {
        updateProgressMeter(0);
        pMeter.setVisible(false);
    }
    
    public void disableControls1() {
        controls1.setSize(CONTROLS_ZERO_SIZE);
    }
    
    public void disableControls2() {
        controls2.setSize(CONTROLS_ZERO_SIZE);
    }
    
    public void enableControls1() {
        controls1.setSize(CONTROLS1_SIZE);
    }
    
    public void enableControls2() {
        controls2.setSize(CONTROLS2_SIZE);
    }

    public void updateProgressMeter(int value) {
        pMeter.setValue(value);
    }
    
    public void enableNextButton() {
        this.next.setVisible(true);
    }
    
    public void disableNextButton() {
        this.next.setVisible(false);
    }
    
    public void enablePrevButton() {
        this.prev.setVisible(true);
    }
    
    public void disablePrevButton() {
        this.prev.setVisible(false);
    }
}
