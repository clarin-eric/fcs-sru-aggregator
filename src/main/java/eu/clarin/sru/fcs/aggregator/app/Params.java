package eu.clarin.sru.fcs.aggregator.app;

import java.util.concurrent.TimeUnit;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 *
 * @author edima
 */
public class Params {
	public final int cacheMaxDepth;
	public final TimeUnit cacheUpdateIntervalUnit;
	public final int cacheUpdateInterval;
	public final String dataLocationPropertyName;
	public final String aggregatorDirName;


	public Params() throws NamingException {
		InitialContext context = new InitialContext();

		cacheMaxDepth = (Integer) context.lookup("java:comp/env/scan-max-depth");

		String updateIntervalUnitString = (String) context.lookup("java:comp/env/update-interval-unit");
		cacheUpdateIntervalUnit = TimeUnit.valueOf(updateIntervalUnitString);

		cacheUpdateInterval = (Integer) context.lookup("java:comp/env/update-interval");

		dataLocationPropertyName = (String) context.lookup("java:comp/env/data-location-property");

		aggregatorDirName = (String) context.lookup("java:comp/env/aggregator-folder");
	}

}
