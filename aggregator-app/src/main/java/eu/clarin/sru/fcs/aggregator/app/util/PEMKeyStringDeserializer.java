package eu.clarin.sru.fcs.aggregator.app.util;

import java.io.IOException;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * Helper to correctly deserialize PEM keys when e.g., supplied via environment
 * variable substitution which doesn't correctly preserve the line breaks.
 */
public final class PEMKeyStringDeserializer extends JsonDeserializer<String> {
    private final static String MARKER_DELIMS = "-----";
    private final static int MARKER_DELIMS_LEN = MARKER_DELIMS.length();

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException, JacksonException {
        String contents = p.getText().strip();
        if (!contents.startsWith(MARKER_DELIMS) || !contents.endsWith(MARKER_DELIMS)) {
            throw new JsonParseException(p, "Expected PEM key block with '-----' delimiters!");
        }
        if (contents.indexOf('\n') != -1) {
            // has line breaks, we do nothing
            return contents;
        }
        // no line breaks
        if (contents.indexOf("\\n") != -1) {
            contents = contents.replace("\\n", "\n");
            return contents;
        }
        // probably line breaks converted to single spaces
        int posStartOfStartBlock = contents.indexOf(MARKER_DELIMS + "BEGIN ");
        if (posStartOfStartBlock == -1) {
            throw new JsonParseException(p,
                    "Unexpected PEM structure, expected '-----BEGIN ' start of BEGIN block indicator!");
        }
        int posEndOfStartBlock = contents.indexOf(MARKER_DELIMS, posStartOfStartBlock + MARKER_DELIMS_LEN + 6 + 1);
        if (posEndOfStartBlock == -1) {
            throw new JsonParseException(p,
                    "Unexpected PEM structure, expected '----- ' end of BEGIN block indicator!");
        }

        int posStartOfEndBlock = contents.indexOf(MARKER_DELIMS + "END ", posEndOfStartBlock + MARKER_DELIMS_LEN + 1);
        if (posStartOfEndBlock == -1) {
            throw new JsonParseException(p,
                    "Unexpected PEM structure, expected '-----END ' start of END block indicator!");
        }
        int posEndOfEndBlock = contents.indexOf(MARKER_DELIMS, posStartOfEndBlock + MARKER_DELIMS_LEN + 4 + 1);
        if (posEndOfEndBlock == -1) {
            throw new JsonParseException(p, "Unexpected PEM structure, expected '-----' end of END block indicator!");
        }

        int posStartContents = posEndOfStartBlock + MARKER_DELIMS_LEN;
        int posEndContents = posStartOfEndBlock;

        String header = contents.substring(0, posEndOfStartBlock + MARKER_DELIMS_LEN);
        String footer = contents.substring(posStartOfEndBlock);
        String blockContents = contents.substring(posStartContents, posEndContents).strip().replace(' ', '\n');
        return header + "\n" + blockContents + "\n" + footer;
    }
}
