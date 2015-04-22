package eu.clarin.sru.fcs.aggregator.scan;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author edima
 *
 * A Json representation of an exception in the scan/search process, to be sent
 * to the JS client
 */
public class JsonException {

	@JsonProperty
	String klass;

	@JsonProperty
	String message;

	@JsonProperty
	String cause;

	public JsonException(Throwable xc) {
		this.klass = xc.getClass().getCanonicalName();
		this.message = "" + xc.getMessage();
		Throwable xc2 = xc.getCause();
		if (xc2 != null && !xc.getMessage().equals(xc2.getMessage())) {
			this.cause = "" + xc2.getMessage();
		}
	}

	@Override
	public int hashCode() {
		return 67 * klass.hashCode() + 59 * message.hashCode()
				+ (cause == null ? 0 : 13 * cause.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof JsonException)) {
			return false;
		}
		JsonException e = (JsonException) obj;
		return klass.equals(e.klass) && message.equals(e.message)
				&& ((cause == null && e.cause == null) || (cause != null && cause.equals(e.cause)));
	}
}
