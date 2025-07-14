package eu.clarin.sru.fcs.aggregator.util;

import java.util.HashMap;
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

    // ---------------------------------------------------------------------

    public static Map<String, String> mapLanguageCodes3To1(Map<String, String> map, LanguagesISO693 languages) {
        if (map == null) {
            return map;
        }
        if (languages == null) {
            return map;
        }

        // check if info has language codes with 3 digits
        boolean needsConverting = false;
        for (String language : map.keySet()) {
            if (language.length() != 2) {
                needsConverting = true;
                break;
            }
        }
        if (!needsConverting) {
            return map;
        }

        // map codes with 3 digits to 2-digits codes if possible
        // xml:lang is BCP47 / should use shortest code from ISCO639 code variants
        Map<String, String> mapConverted = new HashMap<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String language = entry.getKey();
            if (language.length() == 3) {
                language = languages.code_1ForCode_3(language);
                if (language == null) {
                    language = entry.getKey();
                }
            }
            mapConverted.put(language, entry.getValue());
        }
        return mapConverted;
    }

}
