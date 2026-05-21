package eu.clarin.sru.fcs.aggregator.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

public class LanguagesISO693Test {

    @Test
    public void testLanguagesISO693Instance() {
        LanguagesISO693 languages = LanguagesISO693.getInstance();
        assertNotNull(languages, "LanguagesISO693 instance not null");

        Map<String, ?> entries = languages.getCodeToLangMap();
        assertTrue(entries.size() > 0, "there should be language entries available");

        String firstCode = entries.keySet().iterator().next();
        assertTrue(languages.isCode(firstCode), "code should be valid code");

        String name = languages.nameForCode(firstCode);
        assertNotNull(name, "language name must not be null");

        // String code1 = languages.code_1ForCode_3(firstCode);
        // assertNotNull(code1, "code 1 should probably not be null?");
        // String code3 = languages.code_3ForCode(code1);
        // assertNotNull(code3, "code 3 should probably not be null?");
    }

}
