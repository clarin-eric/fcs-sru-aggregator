package eu.clarin.sru.fcs.aggregator.scan;

import java.io.Serializable;

/**
 * A representation of a Java exception in the scan/search process, to be sent
 * to the JS client.
 *
 * @author edima
 */
public class JavaException implements Serializable {
    private static final long serialVersionUID = 2144004963276094441L;

    public final String klass;
    public final String message;
    public final String cause;

    public JavaException(Throwable xc) {
        this.klass = xc.getClass().getCanonicalName();
        this.message = xc.getMessage();
        Throwable xc2 = xc.getCause();
        if (xc2 != null && !xc.getMessage().equals(xc2.getMessage())) {
            this.cause = xc2.getMessage();
        } else {
            cause = null;
        }
    }

    @Override
    public int hashCode() {
        return 67 * klass.hashCode() + 59 * message.hashCode()
                + (cause == null ? 0 : 13 * cause.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof JavaException)) {
            return false;
        }
        JavaException e = (JavaException) obj;
        return klass.equals(e.klass) && message.equals(e.message)
                && ((cause == null && e.cause == null) || (cause != null && cause.equals(e.cause)));
    }
}
