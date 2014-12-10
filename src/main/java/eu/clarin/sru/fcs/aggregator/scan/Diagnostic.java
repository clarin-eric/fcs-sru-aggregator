package eu.clarin.sru.fcs.aggregator.scan;

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

	@Override
	public int hashCode() {
		https://primes.utm.edu/lists/small/1000.txt
		return uri.hashCode() * 967 + context.hashCode() * 797
				+ message.hashCode() * 1669 + diagnostic.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Diagnostic)) {
			return false;
		}
		Diagnostic d = (Diagnostic) obj;
		return uri.equals(d.uri) && message.equals(d.message)
				&& context.equals(d.context) && diagnostic.equals(d.diagnostic);
	}
}
