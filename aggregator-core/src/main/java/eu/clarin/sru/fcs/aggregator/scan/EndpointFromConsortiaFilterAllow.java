package eu.clarin.sru.fcs.aggregator.scan;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Filter to only allow endpoints which belong to a allowed set of consortia. If
 * no consortia is known for an endpoint it is allowed by default (by default).
 * 
 * @author ekoerner
 */
public class EndpointFromConsortiaFilterAllow implements EndpointFilter {

    private Set<String> allow = new HashSet<String>();
    private boolean defaultForNull = true;

    public EndpointFromConsortiaFilterAllow(String... consortia) {
        Collections.addAll(allow, consortia);
    }

    public EndpointFromConsortiaFilterAllow(boolean defaultForNull, String... consortia) {
        this.defaultForNull = defaultForNull;
        Collections.addAll(allow, consortia);
    }

    @Override
    public boolean filterConsortium(String consortium) {
        if (consortium == null) {
            return defaultForNull;
        }
        return allow.contains(consortium);
    }
}
