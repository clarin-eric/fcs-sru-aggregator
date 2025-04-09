package eu.clarin.sru.fcs.aggregator.scan;

import eu.clarin.weblicht.bindings.cmd.cp.CenterProfile;

public interface CentreFilter {
    /**
     * @return true if the centre should be considered for endpoint check, false
     *         otherwise
     */
    boolean filter(CenterProfile profile);
}
