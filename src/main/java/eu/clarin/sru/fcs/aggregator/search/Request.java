package eu.clarin.sru.fcs.aggregator.search;

import eu.clarin.sru.fcs.aggregator.registry.Corpus;
import java.util.logging.Logger;

/**
 * @author edima
 */

public class Request {
	private static final Logger LOGGER = Logger.getLogger(Request.class.getName());

	private Corpus corpus;
	private String searchString;
	private int startRecord;
	private int endRecord;

	public Request(Corpus corpus, String searchString, int startRecord, int endRecord) {
		this.corpus = corpus;
		this.searchString = searchString;
		this.startRecord = startRecord;
		this.endRecord = endRecord;
	}

	public int getStartRecord() {
		return startRecord;
	}

	public int getEndRecord() {
		return endRecord;
	}

	public Corpus getCorpus() {
		return corpus;
	}

	public String getSearchString() {
		return searchString;
	}

	public boolean hasCorpusHandler() {
		if (corpus != null && corpus.getHandle() != null) {
			return true;
		}
		return false;
	}
}
