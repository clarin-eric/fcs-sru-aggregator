package eu.clarin.sru.fcs.aggregator.search;

import com.googlecode.sardine.Sardine;
import com.googlecode.sardine.SardineFactory;
import com.googlecode.sardine.impl.SardineException;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import java.util.logging.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * @author Yana Panchenko
 * @author edima
 */
public class DataTransfer {

	private static final Logger LOGGER = Logger.getLogger(DataTransfer.class.getName());

	private static final String WSPACE_SERVER_URL = "http://egi-cloud21.zam.kfa-juelich.de";
	private static final String WSPACE_WEBDAV_DIR = "/owncloud/remote.php/webdav/";
	private static final String WSPACE_AGGREGATOR_DIR = "aggregator_results/";
	private static final String DROP_OFF_URL = "http://ws1-clarind.esc.rzg.mpg.de/drop-off/storage/";

	static void uploadToPW(String user, String pass, byte[] bytes, String mimeType, String fileExtention) {
		try {
			Sardine sardine = SardineFactory.begin();
			sardine.setCredentials(user, pass);
			String outputDir = WSPACE_SERVER_URL + WSPACE_WEBDAV_DIR + WSPACE_AGGREGATOR_DIR;
			if (!sardine.exists(outputDir)) {
				sardine.createDirectory(outputDir);
			}
			Date currentDate = new Date();
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
			Random generator = new Random();
			int rn1 = generator.nextInt(1000000000);
			String createdFilePath = outputDir + format.format(currentDate) + "-" + rn1 + fileExtention;
			while (sardine.exists(createdFilePath)) {
				rn1 = generator.nextInt(1000000000);
				createdFilePath = outputDir + format.format(currentDate) + "-" + rn1 + fileExtention;
			}
			sardine.put(createdFilePath, bytes, mimeType);
			// "Export complete!\nCreated file:\n" + createdFilePath
		} catch (SardineException ex) {
			LOGGER.log(Level.SEVERE, "Error accessing " + WSPACE_SERVER_URL + WSPACE_WEBDAV_DIR, ex);
			//"Wrong name or password!"
		} catch (IOException ex) {
			LOGGER.log(Level.SEVERE, "Error exporting {0} {1} {2}", new String[]{fileExtention, ex.getClass().getName(), ex.getMessage()});
			//"Sorry, export error!"
		}
	}

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
