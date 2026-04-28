package eu.clarin.sru.fcs.aggregator.scan;

import java.net.URL;
import java.util.Map;

public interface EndpointConfig {

    URL getUrl();

    Map<String, String> getName();

    String getWebsite();

    public final static String DEFAULT_NAME_LANGUAGE = "en";

    public default String getPrimaryName() {
        final Map<String, String> name = getName();

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
}
