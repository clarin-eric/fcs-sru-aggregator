/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.clarin.sru.fcs.aggregator.app;

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
    
    private static final String CONTROLS1_SIZE = "30px";
    private static final String CONTROLS2_SIZE = "25px";
    private static final String CONTROLS_ZERO_SIZE = "0px";

    public ControlsVisibility(North controls1, South controls2, Progressmeter pMeter, Menubar menubar) {
        this.controls1 = controls1;
        this.controls2 = controls2;
        this.pMeter = pMeter;
        this.menubar = menubar;
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
        pMeter.setVisible(true);
        updateProgressMeter(value);
    }
    
    public void disableProgressMeter() {
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
}
