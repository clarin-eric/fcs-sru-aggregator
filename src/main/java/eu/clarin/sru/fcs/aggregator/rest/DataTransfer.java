package eu.clarin.sru.fcs.aggregator.rest;

import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.setup.Environment;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.LoggerFactory;

/**
 * A utility class that allows moving data from the Aggregator server to some
 * public location (currently the drop-off service).
 * 
 * @author Yana Panchenko
 * @author edima
 */
public class DataTransfer {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(DataTransfer.class);

    private static final String WSPACE_SERVER_URL = "http://egi-cloud21.zam.kfa-juelich.de";
    private static final String WSPACE_WEBDAV_DIR = "/owncloud/remote.php/webdav/";
    private static final String WSPACE_AGGREGATOR_DIR = "aggregator_results/";
    private static final String DROP_OFF_URL = "http://ws1-clarind.esc.rzg.mpg.de/drop-off/storage/";

    static String uploadToDropOff(byte[] bytes, String mimeType, String fileExtention, Environment env) {
        Client client = null;
        String url = null;
        try {
            Date currentDate = new Date();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
            Random generator = new Random();
            int rn1 = generator.nextInt(1000000000);
            String createdFileName = format.format(currentDate) + "-" + rn1 + fileExtention;

            JerseyClientConfiguration config = new JerseyClientConfiguration();
            client = new JerseyClientBuilder(env).using(config).build("DataTransfer");

            url = DROP_OFF_URL + createdFileName;
            WebTarget service = client.target(url);

            Response response = service
                    .request(mimeType)
                    .post(Entity.entity(bytes, MediaType.APPLICATION_OCTET_STREAM));
            if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
                log.error("Error uploading {}", url);
                // "Sorry, export to drop-off error!"
                return null;
            }
        } catch (Exception ex) {
            log.error("Error uploading {} : {} {}", url, ex.getClass().getName(), ex.getMessage());
            // "Sorry, export to drop-off error!"
            return null;
        } finally {
            if (client != null) {
                client.close();
            }
        }
        return url;

    }
}
