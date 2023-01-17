package eu.clarin.sru.fcs.aggregator.scan;

/**
 * @author edima
 */
public class Endpoint {

    private String url;
    private FCSProtocolVersion protocol;

    // for JSON deserialization
    public Endpoint() {
    }

    public Endpoint(String url, FCSProtocolVersion protocol) {
        this.url = url;
        this.protocol = protocol;
    }

    public String getUrl() {
        return url;
    }

    public FCSProtocolVersion getProtocol() {
        return protocol;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setProtocol(FCSProtocolVersion protocol) {
        this.protocol = protocol;
    }

    @Override
    public String toString() {
        return url;
    }

    @Override
    public int hashCode() {
        // https://primes.utm.edu/lists/small/1000.txt
        return url.hashCode() * 1049;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Endpoint)) {
            return false;
        }
        Endpoint e = (Endpoint) obj;
        return e.url.equals(url) && e.protocol.equals(protocol);
    }
}
