package eu.clarin.sru.fcs.aggregator.search;

import java.util.List;

import eu.clarin.sru.client.fcs.DataViewLex;

public class LexEntry {

    private List<DataViewLex.Field> fields;
    private String pid;
    private String reference;
    private String lang;
    private String langUri;

    public LexEntry(final DataViewLex dv, String pid, String reference) {
        this.fields = dv.getFields();
        this.lang = dv.getXmlLang();
        this.langUri = dv.getLangUri();
        this.pid = pid;
        this.reference = reference;
    }

    public List<DataViewLex.Field> getFields() {
        return fields;
    }

    public String getPid() {
        return pid;
    }

    public String getReference() {
        return reference;
    }

    public String getLang() {
        return lang;
    }

    public String getLangUri() {
        return langUri;
    }

}
