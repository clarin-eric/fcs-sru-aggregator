package eu.clarin.sru.fcs.aggregator.search;

import eu.clarin.sru.client.fcs.DataViewKWIC;

/**
 * Represents keyword in context data view and information about its
 * PID and reference.
 * 
 * @author Yana Panchenko
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
