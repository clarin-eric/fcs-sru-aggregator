package eu.clarin.sru.fcs.aggregator.registry;

/*
 * @author edima
 */
public class Diagnostic {

	private String uri;
	private String context;
	private String message;
	private String diagnostic;

	public Diagnostic(String uri, String context, String message, String diagnostic) {
		this.uri = uri;
		this.context = context;
		this.message = message;
		this.diagnostic = diagnostic;
	}

	public Diagnostic() {
	}

	public String getUri() {
		return uri;
	}

	public String getContext() {
		return context;
	}
	public String getMessage() {
		return message;
	}

	public String getDiagnostic() {
		return diagnostic;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public void setContext(String context) {
		this.context = context;
	}

	public void setDiagnostic(String diagnostic) {
		this.diagnostic = diagnostic;
	}
}
