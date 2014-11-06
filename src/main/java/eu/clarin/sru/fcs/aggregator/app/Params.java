package eu.clarin.sru.fcs.aggregator.app;

import java.util.concurrent.TimeUnit;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.slf4j.LoggerFactory;

/**
 *
 * @author edima
 */
public class Params {

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(Params.class);

	public final String centerRegistryUrl;
	public final TimeUnit cacheUpdateIntervalUnit;
	public final int cacheUpdateInterval;
	public int cacheMaxDepth;
	public String aggregatorFilePath;

	public Params() throws NamingException {
		InitialContext context = new InitialContext();

		centerRegistryUrl = (String) context.lookup("java:comp/env/center-registry-url");

		cacheMaxDepth = (Integer) context.lookup("java:comp/env/scan-max-depth");

		String updateIntervalUnitString = (String) context.lookup("java:comp/env/update-interval-unit");
		cacheUpdateIntervalUnit = TimeUnit.valueOf(updateIntervalUnitString);

		cacheUpdateInterval = (Integer) context.lookup("java:comp/env/update-interval");

		aggregatorFilePath = (String) context.lookup("java:comp/env/aggregator-file-path");

		log.info("centerRegistryUrl = {}", centerRegistryUrl);
		log.info("cacheMaxDepth = {}", cacheMaxDepth);
		log.info("cacheUpdateInterval = {} {}", cacheUpdateInterval, cacheUpdateIntervalUnit);
		log.info("aggregatorFilePath = {}", aggregatorFilePath);
	}
}
