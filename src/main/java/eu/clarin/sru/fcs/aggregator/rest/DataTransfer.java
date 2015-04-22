package eu.clarin.sru.fcs.aggregator.rest;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import java.util.logging.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * @author Yana Panchenko
 * @author edima
 *
 * A utility class that allows moving data from the Aggregator server to some
 * public location (currently the drop-off service).
 */
public class DataTransfer {

	private static final Logger LOGGER = Logger.getLogger(DataTransfer.class.getName());

	private static final String WSPACE_SERVER_URL = "http://egi-cloud21.zam.kfa-juelich.de";
	private static final String WSPACE_WEBDAV_DIR = "/owncloud/remote.php/webdav/";
	private static final String WSPACE_AGGREGATOR_DIR = "aggregator_results/";
	private static final String DROP_OFF_URL = "http://ws1-clarind.esc.rzg.mpg.de/drop-off/storage/";

	static String uploadToDropOff(byte[] bytes, String mimeType, String fileExtention) {
		Client client = null;
		String url = null;
		try {
			Date currentDate = new Date();
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
			Random generator = new Random();
			int rn1 = generator.nextInt(1000000000);
			String createdFileName = format.format(currentDate) + "-" + rn1 + fileExtention;

			ClientConfig config = new DefaultClientConfig();
			client = Client.create(config);
			url = DROP_OFF_URL + createdFileName;
			WebResource service = client.resource(url);

			ClientResponse response = service.type(mimeType) //.accept(MediaType.TEXT_PLAIN).post(String.class, media.getStringData());
					.post(ClientResponse.class, bytes);
			if (response.getClientResponseStatus() != ClientResponse.Status.CREATED) {
				LOGGER.log(Level.SEVERE, "Error uploading {0}", new String[]{url});
				//"Sorry, export to drop-off error!"
				return null;
			}
		} catch (Exception ex) {
			LOGGER.log(Level.SEVERE, "Error uploading {0} {1} {2}", new String[]{url, ex.getClass().getName(), ex.getMessage()});
			//"Sorry, export to drop-off error!"
			return null;
		} finally {
			if (client != null) {
				client.destroy();
			}
		}
		return url;

	}
}
