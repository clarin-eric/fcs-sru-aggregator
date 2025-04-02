package eu.clarin.sru.fcs.aggregator.search;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.text.StringEscapeUtils;

import eu.clarin.sru.client.fcs.DataViewHits;
import eu.clarin.sru.client.fcs.DataViewHitsWithLexAnnotations;

/**
 * Represents keyword in context data view and information about its PID and
 * reference.
 *
 * @author Yana Panchenko
 */
public class Kwic {

    public static class TextFragment {

        private final String text;
        private final boolean isHit;
        private final String hitKind;

        public TextFragment(String text, boolean isHit) {
            this(text, isHit, null);
        }

        public TextFragment(String text, boolean isHit, String hitKind) {
            this.text = text;
            this.isHit = isHit;
            this.hitKind = hitKind;
        }

        public String getText() {
            return text;
        }

        public boolean isHit() {
            return isHit;
        }

        public String getHitKind() {
            return hitKind;
        }

        public boolean hasHitKind() {
            return hitKind != null;
        }

        @Override
        public String toString() {
            return (isHit ? "[" : "") + text + (isHit ? "]" : "");
        }
    }

    private final String pid;
    private final String reference;
    private String language;
    private List<TextFragment> fragments = new ArrayList<TextFragment>();

    public Kwic(DataViewHits hits, String pid, String reference) {
        this.pid = pid;
        this.reference = reference;

        // warning: the client library doesn't unescape the xml
        // so the text can still contains &lt; and &amp; codes
        final String str = hits.getText();

        int lastOffset = 0;
        for (int i = 0; i < hits.getHitCount(); i++) {
            int[] offsets = hits.getHitOffsets(i);
            if (lastOffset < offsets[0]) {
                String text = StringEscapeUtils.unescapeXml(str.substring(lastOffset, offsets[0]));
                fragments.add(new TextFragment(text, false));
            }
            if (offsets[0] < offsets[1]) {
                String text = StringEscapeUtils.unescapeXml(str.substring(offsets[0], offsets[1]));
                fragments.add(new TextFragment(text, true,
                        (hits instanceof DataViewHitsWithLexAnnotations)
                                ? ((DataViewHitsWithLexAnnotations) hits).getHitKind(i)
                                : null));
            }
            lastOffset = offsets[1];
        }
        if (lastOffset < str.length()) {
            String text = StringEscapeUtils.unescapeXml(str.substring(lastOffset, str.length()));
            fragments.add(new TextFragment(text, false));
        }
    }

    public List<TextFragment> getFragments() {
        return fragments;
    }

    public String getPid() {
        return pid;
    }

    public String getReference() {
        return reference;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @Deprecated
    public String getLeft() {
        StringBuilder sb = new StringBuilder();
        for (TextFragment tf : fragments) {
            if (tf.isHit) {
                break;
            }
            sb.append(tf.text);
        }
        return sb.toString();
    }

    @Deprecated
    public String getKeyword() {
        for (TextFragment tf : fragments) {
            if (tf.isHit) {
                return tf.text;
            }
        }
        return "";
    }

    @Deprecated
    public String getRight() {
        StringBuilder sb = new StringBuilder();
        boolean pastHit = false;
        for (TextFragment tf : fragments) {
            if (pastHit) {
                sb.append(tf.text);
            }
            if (tf.isHit) {
                pastHit = true;
            }
        }
        return sb.toString();
    }
}
