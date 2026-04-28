package eu.clarin.sru.fcs.aggregator.scan;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Institution. Contains information about institution name and link (url). Can
 * have information about its CQL Endpoints. May contain the country code.
 *
 * @author Yana Panchenko
 */
public class Institution {

    public final static String DEFAULT_NAME_LANGUAGE = "en";

    private Map<String, String> name;
    private String link;
    private String consortium;
    private Set<Endpoint> endpoints = new LinkedHashSet<>();
    private boolean sideloaded = false;

    // for JSON deserialization
    public Institution() {
    }

    public Institution(String name, String link) {
        this(name, link, null, false);
    }

    public Institution(Map<String, String> name, String link) {
        this(name, link, null, false);
    }

    public Institution(String name, String link, String consortium) {
        this(name, link, consortium, false);
    }

    public Institution(Map<String, String> name, String link, String consortium) {
        this(name, link, consortium, false);
    }

    public Institution(String name, String link, boolean sideloaded) {
        this(name, link, null, sideloaded);
    }

    public Institution(Map<String, String> name, String link, boolean sideloaded) {
        this(name, link, null, sideloaded);
    }

    public Institution(String name, String link, String consortium, boolean sideloaded) {
        this(Map.of(DEFAULT_NAME_LANGUAGE, name), link, consortium, sideloaded);
    }

    public Institution(Map<String, String> name, String link, String consortium, boolean sideloaded) {
        this.name = name;
        this.link = link;
        this.consortium = consortium;
        this.sideloaded = sideloaded;
    }

    public String addEndpoint(String endpointUrl) {
        endpoints.add(new Endpoint(endpointUrl, FCSProtocolVersion.LEGACY));
        return endpointUrl;
    }

    public String addEndpoint(String endpointUrl, FCSProtocolVersion version) {
        if (version.equals(FCSProtocolVersion.VERSION_2)) {
            endpoints.add(new Endpoint(endpointUrl, FCSProtocolVersion.VERSION_2));
        } else if (version.equals(FCSProtocolVersion.VERSION_1)) {
            endpoints.add(new Endpoint(endpointUrl, FCSProtocolVersion.VERSION_1));
        } else if (version.equals(FCSProtocolVersion.LEGACY)) {
            endpoints.add(new Endpoint(endpointUrl, FCSProtocolVersion.LEGACY));
        } else {
            endpoints.add(new Endpoint(endpointUrl, FCSProtocolVersion.LEGACY));
        }
        return endpointUrl;
    }

    public Map<String, String> getName() {
        return name;
    }

    public String getPrimaryName() {
        if (name == null || name.isEmpty()) {
            return null;
        }

        String nameEn = name.getOrDefault(DEFAULT_NAME_LANGUAGE, null);
        if (nameEn != null && nameEn.length() > 0) {
            return nameEn;
        }

        String firstLang = name.keySet().iterator().next();
        String nameXx = name.getOrDefault(firstLang, null);
        if (nameXx != null && nameXx.length() > 0) {
            return nameXx;
        }

        return null;
    }

    public String getLink() {
        return link;
    }

    public String getConsortium() {
        return consortium;
    }

    public Set<Endpoint> getEndpoints() {
        return this.endpoints;
    }

    public boolean isSideloaded() {
        return sideloaded;
    }

    @Override
    public String toString() {
        String nameXx = getPrimaryName();
        if (nameXx != null && nameXx.length() > 0) {
            return nameXx;
        }
        return link;
    }

    @Override
    public int hashCode() {
        String nameXx = getPrimaryName();
        if (nameXx == null) {
            return 0;
        }
        // https://primes.utm.edu/lists/small/1000.txt
        return nameXx.hashCode() * 2953;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Institution)) {
            return false;
        }
        Institution i = (Institution) obj;
        if (i.name == null) {
            return this.name == null;
        }
        return i.name.equals(name);
    }
}
