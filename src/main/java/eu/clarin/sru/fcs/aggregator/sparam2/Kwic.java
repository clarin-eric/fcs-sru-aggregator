/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.clarin.sru.fcs.aggregator.sparam2;

import eu.clarin.sru.client.fcs.DataViewKWIC;

/**
 *
 * @author Yana Panchenko <yana_panchenko at yahoo.com>
 */
public class Kwic {
    
    private DataViewKWIC kw;
    private String pid;
    private String reference;

    public Kwic(DataViewKWIC kw, String pid, String reference) {
        this.kw = kw;
        this.pid = pid;
        this.reference = reference;
    }

    public String getLeft() {
        return kw.getLeft();
    }
    
    public String getKeyword() {
        return kw.getKeyword();
    }
    
    public String getRight() {
        return kw.getRight();
    }

    public String getPid() {
        return pid;
    }

    public String getReference() {
        return reference;
    }

}
