package eu.clarin.sru.fcs.aggregator.app;

import eu.clarin.sru.fcs.aggregator.registry.CenterRegistry;
import eu.clarin.sru.fcs.aggregator.registry.CenterRegistryLive;
import eu.clarin.sru.fcs.aggregator.registry.Institution;
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
			CenterRegistry centerRegistry = new CenterRegistryLive(centerRegistryUrl);
			for (Institution institution : centerRegistry.getCQLInstitutions()) {
				System.out.println(institution.getName() + ": ");
				for (String e : institution.getEndpoints()) {
					System.out.println("\t -> " + e);
				}
			}
			Assert.assertTrue(centerRegistry.getCQLInstitutions().size() > 10);
		} finally {
		}
	}
}
