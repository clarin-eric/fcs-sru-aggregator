package eu.clarin.sru.fcs.aggregator.search;

import java.util.List;

public class ResultRecord implements Record {
    private Kwic kwic;
    private List<AdvancedLayer> advancedLayers;
    private LexEntry lexEntry;

    private String language;

    @Override
    public boolean hasResult() {
        return kwic != null;
    }

    @Override
    public boolean hasDiagnostic() {
        return false;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Kwic getKwic() {
        return kwic;
    }

    public void setKwic(Kwic kwic) {
        this.kwic = kwic;
    }

    public List<AdvancedLayer> getAdvancedLayers() {
        return advancedLayers;
    }

    public void setAdvancedLayers(List<AdvancedLayer> advancedLayers) {
        this.advancedLayers = advancedLayers;
    }

    public boolean hasAdvancedLayers() {
        return advancedLayers != null && !advancedLayers.isEmpty();
    }

    public LexEntry getLexEntry() {
        return lexEntry;
    }

    public void setLexEntry(LexEntry lexEntry) {
        this.lexEntry = lexEntry;
    }

    public boolean hasLexEntry() {
        return lexEntry != null;
    }
}