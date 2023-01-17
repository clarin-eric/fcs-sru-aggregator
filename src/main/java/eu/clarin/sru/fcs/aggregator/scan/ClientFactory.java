package eu.clarin.sru.fcs.aggregator.scan;

/*
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.jersey.client.apache4.ApacheHttpClient4;
import com.sun.jersey.client.apache4.ApacheHttpClient4Handler;
*/
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.ws.rs.client.Client;
import org.glassfish.jersey.client.ClientProperties;

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
 *         Shamelessly copied from the weblicht Harvester by emanueldima
 */
public class ClientFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientFactory.class);

    public static Client create(int connectTimeout, int readTimeout, Environment env) {
        /*
         * SSLContext sc = null;
         * try {
         * sc = SSLContext.getInstance("SSL");
         * sc.init(null, trustAllCerts, new SecureRandom());
         * } catch (NoSuchAlgorithmException e) {
         * LOGGER.error("Unknown algorithm, SSL: {}", e.getMessage());
         * } catch (KeyManagementException e) {
         * LOGGER.error("Key management problem: {}", e.getMessage());
         * }
         * 
         * HttpClient httpClient = HttpClientBuilder.create()
         * .setRedirectStrategy(new LaxRedirectStrategy())
         * .setSslcontext(sc)
         * .build();
         */
        JerseyClientConfiguration config = new JerseyClientConfiguration();
        // config.setTlsConfiguration(new TlsConfiguration());
        config.setConnectionTimeout(Duration.milliseconds(connectTimeout));
        config.setTimeout(Duration.milliseconds(readTimeout));

        Client client = new JerseyClientBuilder(env).using(config).build(ClientFactory.class.getName());
        client.property(ClientProperties.FOLLOW_REDIRECTS, Boolean.TRUE);

        return client;
    }

    private static TrustManager[] trustAllCerts = { new X509TrustManager() {
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
    } };

    private ClientFactory() {
    }
}
