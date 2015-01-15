package eu.clarin.sru.fcs.aggregator.search;

import eu.clarin.sru.client.fcs.DataViewHits;
import eu.clarin.sru.fcs.aggregator.app.Aggregator;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents keyword in context data view and information about its PID and
 * reference.
 *
 * @author Yana Panchenko
 */
public class Kwic {

	public static class TextFragment {

		String text;
		boolean isHit;

		public TextFragment(String text, boolean isHit) {
			this.text = text;
			this.isHit = isHit;
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

	private String pid;
	private String reference;
	private String language;
	private List<TextFragment> fragments = new ArrayList<TextFragment>();

	public Kwic(DataViewHits hits, String pid, String reference) {
		this.pid = pid;
		this.reference = reference;

		String text = hits.getText();
		int lastOffset = 0;
		for (int i = 0; i < hits.getHitCount(); i++) {
			int[] offsets = hits.getHitOffsets(i);
			if (lastOffset < offsets[0]) {
				fragments.add(new TextFragment(text.substring(lastOffset, offsets[0]), false));
			}
			if (offsets[0] < offsets[1]) {
				fragments.add(new TextFragment(text.substring(offsets[0], offsets[1]), true));
			}
			lastOffset = offsets[1];
		}
		if (lastOffset < text.length()) {
			fragments.add(new TextFragment(text.substring(lastOffset, text.length()), false));
		}

		language = Aggregator.getInstance().detectLanguage(hits.getText());
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

	@Deprecated
	public String getLeft() {
		for (TextFragment tf : fragments) {
			if (!tf.isHit) {
				return tf.text;
			}
		}
		return "";
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
