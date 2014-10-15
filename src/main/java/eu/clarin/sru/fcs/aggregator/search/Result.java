package eu.clarin.sru.fcs.aggregator.search;

import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRURecord;
import eu.clarin.sru.fcs.aggregator.registry.Corpus;
import eu.clarin.sru.client.SRUSearchRetrieveResponse;
import eu.clarin.sru.client.SRUSurrogateRecordData;
import eu.clarin.sru.client.fcs.ClarinFCSRecordData;
import eu.clarin.sru.client.fcs.DataView;
import eu.clarin.sru.client.fcs.DataViewKWIC;
import eu.clarin.sru.client.fcs.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the results of a SRU search-retrieve operation request. It
 * contains the endpoint/corpus (if specified in the request) to which a request
 * was sent, and the corresponding SRU search-retrieve response.
 *
 * @author Yana Panchenko
 * @author edima
 */
public final class Result {
	private static final Logger LOGGER = Logger.getLogger(Result.class.getName());

	private Request request;
	private List<Kwic> kwics = new ArrayList<Kwic>();
	private SRUClientException exception;

	public List<Kwic> getKwics() {
		return kwics;
	}

	public Result(Request request, SRUSearchRetrieveResponse response, SRUClientException xc) {
		this.request = request;
		this.exception = xc;
		if (response != null) {
			setResponse(response);
		}
	}

	public void setResponse(SRUSearchRetrieveResponse response) {
		for (SRURecord record : response.getRecords()) {
			if (record.isRecordSchema(ClarinFCSRecordData.RECORD_SCHEMA)) {
				ClarinFCSRecordData rd = (ClarinFCSRecordData) record.getRecordData();
				Resource resource = rd.getResource();
				setClarinRecord(resource);
				LOGGER.log(Level.FINE,
						"Resource ref={0}, pid={1}, dataViews={2}",
						new Object[]{resource.getRef(), resource.getPid(), resource.hasDataViews()});
			} else if (record.isRecordSchema(SRUSurrogateRecordData.RECORD_SCHEMA)) {
				SRUSurrogateRecordData r = (SRUSurrogateRecordData) record.getRecordData();
				LOGGER.log(Level.INFO, "Surrogate diagnostic: uri={0}, message={1}, detail={2}",
						new Object[]{r.getURI(), r.getMessage(), r.getDetails()});
			} else {
				LOGGER.log(Level.INFO, "Unsupported schema: {0}", record.getRecordSchema());
			}
		}
	}

	private void setClarinRecord(Resource resource) {
		String pid = resource.getPid();
		String reference = resource.getRef();

		if (resource.hasDataViews()) {
			processDataViews(resource.getDataViews(), pid, reference);
		}

		if (resource.hasResourceFragments()) {
			for (Resource.ResourceFragment fragment : resource.getResourceFragments()) {
				LOGGER.log(Level.FINE, "ResourceFragment: ref={0}, pid={1}, dataViews={2}",
						new Object[]{fragment.getRef(), fragment.getPid(), fragment.hasDataViews()});
				if (fragment.hasDataViews()) {
					processDataViews(fragment.getDataViews(),
							fragment.getPid() != null ? fragment.getPid() : pid,
							fragment.getRef() != null ? fragment.getRef() : reference);
				}
			}
		}
	}

	private void processDataViews(List<DataView> dataViews, String pid, String reference) {
		for (DataView dataview : dataViews) {
			if (dataview.isMimeType(DataViewKWIC.TYPE)) {
				DataViewKWIC kw = (DataViewKWIC) dataview;
				Kwic kwic = new Kwic(kw, pid, reference);
				this.kwics.add(kwic);
				LOGGER.log(Level.FINE, "DataViewKwic: {0} -> {2}", new Object[]{kw.getLeft(), kw.getRight()});
			}
		}
	}

	public SRUClientException getException() {
		return exception;
	}

	public int getStartRecord() {
		return request.getStartRecord();
	}

	public int getEndRecord() {
		return request.getEndRecord();
	}

	public Corpus getCorpus() {
		return request.getCorpus();
	}

	public String getSearchString() {
		return request.getSearchString();
	}

}
