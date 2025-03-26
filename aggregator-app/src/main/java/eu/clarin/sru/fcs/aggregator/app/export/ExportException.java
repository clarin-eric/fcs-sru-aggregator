package eu.clarin.sru.fcs.aggregator.app.export;

/**
 * @author edima
 */
public class ExportException extends Exception {
    private static final long serialVersionUID = 5796638434918111737L;

    public ExportException(String message, Exception ex) {
        super(message, ex);
    }

    public ExportException(String message) {
        super(message);
    }
}
