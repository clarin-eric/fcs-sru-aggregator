package eu.clarin.sru.fcs.aggregator.scan;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.jersey.client.apache4.ApacheHttpClient4;
import com.sun.jersey.client.apache4.ApacheHttpClient4Handler;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Create {@link Client} instances that:
 * <p/>
 * <ul>
 * <li>Follow redirects.</li>
 * <li>Accept any SSL connection.</li>
 * </ul>
 * <p/>
 * We might reconsider the latter feature though ;).
 *
 * @author Daniël de Kok <me@danieldk.eu>
 * Shamelessly copied from the weblicht Harvester by emanueldima
 */
public class ClientFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientFactory.class);

    public static Client create(int connectTimeout, int readTimeout) {
        SSLContext sc = null;
        try {
            sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Unknown algorithm, SSL: {}", e.getMessage());
        } catch (KeyManagementException e) {
            LOGGER.error("Key management problem: {}", e.getMessage());
        }

        HttpClient httpClient = HttpClientBuilder.create()
                .setRedirectStrategy(new LaxRedirectStrategy())
                .setSslcontext(sc)
                .build();

        Client client = new ApacheHttpClient4(new ApacheHttpClient4Handler(httpClient, null, false));
        client.setConnectTimeout(connectTimeout);
        client.setReadTimeout(readTimeout);
        client.setFollowRedirects(true);

        return client;
    }

    private static TrustManager[] trustAllCerts = {new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }};

    private ClientFactory() {
    }
}
