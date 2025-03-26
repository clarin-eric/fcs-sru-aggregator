package eu.clarin.sru.fcs.aggregator.util;

import java.util.UUID;

public final class UniqueId {
    /**
     * Generate random ID based on UUID4.
     *
     * @return new random ID
     */
    public static String generateId() {
        return UUID.randomUUID().toString();
    }
}
