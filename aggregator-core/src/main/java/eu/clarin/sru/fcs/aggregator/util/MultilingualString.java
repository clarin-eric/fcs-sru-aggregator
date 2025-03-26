package eu.clarin.sru.fcs.aggregator.util;

import java.util.Map;

public final class MultilingualString {

    public static String getBestValueFromNullable(Map<String, String> map, String defaultValue) {
        if (map == null || map.isEmpty()) {
            return defaultValue;
        }
        return getBestValueFrom(map);
    }

    public static String getBestValueFromNullable(Map<String, String> map) {
        return getBestValueFromNullable(map, null);
    }

    public static String getBestValueFrom(Map<String, String> map) {
        String ret = map.get("en");
        if (ret == null || ret.trim().isEmpty()) {
            ret = map.get("eng");
        }
        if (ret == null || ret.trim().isEmpty()) {
            ret = map.get(null);
        }
        if (ret == null || ret.trim().isEmpty()) {
            ret = map.get("de");
        }
        if (ret == null || ret.trim().isEmpty()) {
            ret = map.get("deu");
        }
        if (ret == null || ret.trim().isEmpty()) {
            ret = map.size() > 0
                    ? map.values().iterator().next()
                    : null;
        }
        return ret;
    }

}
