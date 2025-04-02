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

        String text;
        String ref;
        boolean isHit;
        DataViewAdvanced.Segment segment;

        public Span(String text, String ref, boolean isHit, DataViewAdvanced.Segment segment) {
            this.text = text;
            this.ref = ref;
            this.isHit = isHit;
            this.segment = segment;
        }

        public String getText() {
            return text;
        }

        public boolean isHit() {
            return isHit;
        }

        @Override
        public String toString() {
            return (isHit ? "[" : "") + text + (isHit ? "]" : "");
        }
    }

    private DataViewAdvanced.Layer layer;
    private String pid;
    private String reference;
    private List<Span> spans = new ArrayList<Span>();

    public AdvancedLayer(DataViewAdvanced.Layer layer, String pid, String reference) {
        this.layer = layer;
        this.pid = pid;
        this.reference = layer.getId();

        for (DataViewAdvanced.Span span : layer.getSpans()) {
            spans.add(new Span(span.getContent(), reference,
                    ("".equals(span.getHighlight()) || span.getHighlight() == null) ? false : true, span.getSegment()));
        }
    }

    public List<Span> getSpans() {
        return spans;
    }

    public String getPid() {
        return pid;
    }

    public String getReference() {
        return reference;
    }

}
