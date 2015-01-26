package eu.clarin.sru.fcs.aggregator.scan;

/*
 * @author edima
 */
public class Diagnostic {

	private String reqEndpointUrl, reqContext;
	private String dgnUri, dgnMessage, dgnDiagnostic;

	public Diagnostic(String reqEndpointUrl, String reqContext, String dgnUri, String dgnMessage, String dgnDiagnostic) {
		this.reqEndpointUrl = reqEndpointUrl;
		this.reqContext = reqContext;
		this.dgnUri = dgnUri;
		this.dgnMessage = dgnMessage;
		this.dgnDiagnostic = dgnDiagnostic;
	}

	public Diagnostic() {
	}

	public String getDgnDiagnostic() {
		return dgnDiagnostic;
	}

	public String getDgnMessage() {
		return dgnMessage;
	}

	public String getDgnUri() {
		return dgnUri;
	}

	public String getReqContext() {
		return reqContext;
	}

	public String getReqEndpointUrl() {
		return reqEndpointUrl;
	}

	public void setDgnDiagnostic(String dgnDiagnostic) {
		this.dgnDiagnostic = dgnDiagnostic;
	}

	public void setDgnMessage(String dgnMessage) {
		this.dgnMessage = dgnMessage;
	}

	public void setDgnUri(String dgnUri) {
		this.dgnUri = dgnUri;
	}

	public void setReqContext(String reqContext) {
		this.reqContext = reqContext;
	}

	public void setReqEndpointUrl(String reqEndpointUrl) {
		this.reqEndpointUrl = reqEndpointUrl;
	}

	@Override
	public int hashCode() {
		https://primes.utm.edu/lists/small/1000.txt
		return reqEndpointUrl.hashCode() * 967 + reqContext.hashCode() * 797
				+ dgnUri.hashCode() * 1669 + dgnMessage.hashCode() * 31
				+ dgnDiagnostic.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Diagnostic)) {
			return false;
		}
		Diagnostic d = (Diagnostic) obj;
		return reqEndpointUrl.equals(d.reqEndpointUrl)
				&& reqContext.equals(d.reqContext)
				&& dgnUri.equals(d.dgnUri)
				&& dgnMessage.equals(d.dgnMessage)
				&& dgnDiagnostic.equals(d.dgnDiagnostic);
	}
}
