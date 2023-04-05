package eu.clarin.sru.fcs.aggregator.util;

/**
 * @author edima
 */
public class Throw {

    public static <T> boolean isCausedBy(Throwable t, Class<T> cause) {
        Throwable xc = t;
        while (xc != null) {
            if (cause.isInstance(xc)) {
                return true;
            } else {
                xc = xc.getCause();
            }
        }
        return false;
    }
}
