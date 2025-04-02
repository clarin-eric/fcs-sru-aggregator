package eu.clarin.sru.fcs.aggregator.search;

import eu.clarin.sru.fcs.aggregator.scan.Diagnostic;

public class DiagnosticRecord implements Record {
    // surrogate diagnostic
    private final Diagnostic diagnostic;

    public DiagnosticRecord(Diagnostic diagnostic) {
        this.diagnostic = diagnostic;
    }

    @Override
    public boolean hasResult() {
        return false;
    }

    @Override
    public boolean hasDiagnostic() {
        return true;
    }

    public Diagnostic getDiagnostic() {
        return diagnostic;
    }
}