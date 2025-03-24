package eu.clarin.sru.fcs.aggregator.scan;

import java.net.MalformedURLException;
import java.net.URL;

import javax.validation.constraints.NotNull;

public class EndpointConfig {
    @NotNull
    private URL url;
    private String name;
    private String website;

    protected EndpointConfig(String url) throws MalformedURLException {
        this.url = new URL(url);
    }

    protected EndpointConfig() {
    }

    public URL getUrl() {
        return url;
    }

    public String getName() {
        return name;
    }

    public String getWebsite() {
        return website;
    }
}
