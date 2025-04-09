package eu.clarin.sru.fcs.aggregator.scan;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import eu.clarin.weblicht.bindings.cmd.cp.CenterBasicInformation;
import eu.clarin.weblicht.bindings.cmd.cp.CenterProfile;
import eu.clarin.weblicht.bindings.cmd.cp.CountryCode;

/**
 * Filter for CLARIN Centre Registry to only retain centres that contain at
 * least one of the provided two letter (upper case) country codes.
 */
public class CountryCentreFilter implements CentreFilter {

    private Set<String> countryCodes = new HashSet<>();

    public CountryCentreFilter(String... countryCodes) {
        Collections.addAll(this.countryCodes, countryCodes);
    }

    @Override
    public boolean filter(CenterProfile profile) {
        CenterBasicInformation info = profile.getCenterBasicInformation();
        List<CountryCode> ccodes = info.getCountry().getCode();
        List<String> codes = ccodes.stream()
                .map(c -> c.getValue()).filter(c -> c != null)
                .map(c -> c.value())
                .collect(Collectors.toList());

        for (String countryCode : countryCodes) {
            if (codes.contains(countryCode)) {
                return true;
            }
        }

        return false;
    }

}
