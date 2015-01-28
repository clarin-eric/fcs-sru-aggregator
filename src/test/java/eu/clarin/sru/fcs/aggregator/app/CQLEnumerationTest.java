package eu.clarin.sru.fcs.aggregator.app;

import eu.clarin.sru.fcs.aggregator.scan.CenterRegistry;
import eu.clarin.sru.fcs.aggregator.scan.CenterRegistryLive;
import eu.clarin.sru.fcs.aggregator.scan.Endpoint;
import eu.clarin.sru.fcs.aggregator.scan.Institution;
import javax.naming.NamingException;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.Assert;

/**
 *
 * @author yanapanchenko
 */
@Ignore
public class CQLEnumerationTest {

	@Test
	public void test() throws NamingException {

		try {
			String centerRegistryUrl = "https://centres-staging.clarin.eu:4430/restxml/";
			CenterRegistry centerRegistry = new CenterRegistryLive(centerRegistryUrl, null);
			for (Institution institution : centerRegistry.getCQLInstitutions()) {
				System.out.println(institution.getName() + ": ");
				for (Endpoint e : institution.getEndpoints()) {
					System.out.println("\t -> " + e);
				}
			}
			Assert.assertTrue(centerRegistry.getCQLInstitutions().size() > 10);
		} finally {
		}
	}
}
