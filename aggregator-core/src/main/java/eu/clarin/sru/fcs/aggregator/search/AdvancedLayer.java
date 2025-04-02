/**
 * @license http://www.gnu.org/licenses/gpl-3.0.txt
 *  GNU General Public License v3
 */
package eu.clarin.sru.fcs.aggregator.search;

import eu.clarin.sru.client.fcs.DataViewAdvanced;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an ADV layer and information about its PID and
 * reference.
 *
 * @author ljo
 */
public class AdvancedLayer {

    public static class Span {
        private String text;
        private boolean isHit;
        private DataViewAdvanced.Segment segment;

        public Span(String text, boolean isHit, DataViewAdvanced.Segment segment) {
            this.text = text;
            this.isHit = isHit;
            this.segment = segment;
        }

        public String getText() {
            return text;
        }

        public boolean isHit() {
            return isHit;
        }

        public long[] getRange() {
            return new long[] { segment.getStartOffset(), segment.getEndOffset() };
        }

        @Override
        public String toString() {
            return (isHit ? "[" : "") + text + (isHit ? "]" : "") + "@" + segment.getStartOffset() + ":"
                    + segment.getEndOffset();
        }
    }

    private String id;
    private List<Span> spans = new ArrayList<Span>();

    public AdvancedLayer(DataViewAdvanced.Layer layer, String pid, String reference) {
        this.id = layer.getId();

        for (DataViewAdvanced.Span span : layer.getSpans()) {
            spans.add(new Span(span.getContent(),
                    ("".equals(span.getHighlight()) || span.getHighlight() == null) ? false : true, span.getSegment()));
        }
    }

    public List<Span> getSpans() {
        return spans;
    }

    public String getId() {
        return id;
    }

}
