package eu.clarin.sru.fcs.aggregator;


public class Corpus {
    private String value;
    private String numberOfRecords;
    private String displayTerm;
    private String lang;
    
        public Corpus() {
        this.value = null;
        this.numberOfRecords = null;
        this.displayTerm = null;
        this.lang = null;
    }

    public Corpus(String value, String numberOfRecords, String displayTerm, String lang) {
        this.value = value;
        this.numberOfRecords = numberOfRecords;
        this.displayTerm = displayTerm;
        this.lang = lang;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getNumberOfRecords() {
        return numberOfRecords;
    }

    public void setNumberOfRecords(String numberOfRecords) {
        this.numberOfRecords = numberOfRecords;
    }

    public String getDisplayTerm() {
        return displayTerm;
    }

    public void setDisplayTerm(String displayTerm) {
        this.displayTerm = displayTerm;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }
    
    
    
    
}
