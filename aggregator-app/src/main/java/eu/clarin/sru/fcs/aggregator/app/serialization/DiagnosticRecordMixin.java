package eu.clarin.sru.fcs.aggregator.app.serialization;

import com.fasterxml.jackson.annotation.JsonProperty;

import eu.clarin.sru.fcs.aggregator.scan.Diagnostic;

public abstract class DiagnosticRecordMixin {
    @JsonProperty("diag")
    Diagnostic diagnostic;
}
