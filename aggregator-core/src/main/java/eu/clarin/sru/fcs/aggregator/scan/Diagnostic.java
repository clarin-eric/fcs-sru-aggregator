package eu.clarin.sru.fcs.aggregator.scan;

/**
 * @author edima
 */
public class Diagnostic {
    String uri;
    String message;
    String diagnostic;

    public Diagnostic(String uri, String message, String diagnostic) {
        this.uri = uri;
        this.message = message;
        this.diagnostic = diagnostic;
    }

    public String getUri() {
        return uri;
    }

    public String getMessage() {
        return message;
    }

    public String getDiagnostic() {
        return diagnostic;
    }

    @Override
    public int hashCode() {
        // https://primes.utm.edu/lists/small/1000.txt
        return uri.hashCode() * 1669 + message.hashCode() * 31
                + diagnostic.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Diagnostic)) {
            return false;
        }
        Diagnostic d = (Diagnostic) obj;
        return uri.equals(d.uri) && message.equals(d.message)
                && diagnostic.equals(d.diagnostic);
    }
}
