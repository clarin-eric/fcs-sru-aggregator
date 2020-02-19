package eu.clarin.sru.fcs.aggregator.app;

import eu.clarin.sru.fcs.aggregator.scan.CenterRegistry;
import eu.clarin.sru.fcs.aggregator.scan.CenterRegistryLive;
import eu.clarin.sru.fcs.aggregator.scan.Endpoint;
import eu.clarin.sru.fcs.aggregator.scan.Institution;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.naming.NamingException;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.Assert;
import org.junit.ClassRule;

/**
 *
 * @author yanapanchenko
 */
@Ignore
public class CQLEnumerationTest {

    @ClassRule
    public static final DropwizardAppRule<AggregatorConfiguration> RULE =
            new DropwizardAppRule<>(Aggregator.class, ResourceHelpers.resourceFilePath("aggregator_devel.yaml"));
    
	public void printAll(String centerRegistryUrl) throws NamingException {
                Environment env = RULE.getEnvironment();
		try {
			CenterRegistry centerRegistry = new CenterRegistryLive(centerRegistryUrl, null, env);
			List<Institution> list = centerRegistry.getCQLInstitutions();
			for (Institution institution : list) {
				System.out.println("1: " + institution.getName() + ": ");
				for (Endpoint e : institution.getEndpoints()) {
					System.out.println("1: \t -> " + e);
				}
			}
			Assert.assertTrue(list.size() > 10);
		} finally {
		}
	}

	@Test
	public void testPrintAll() throws NamingException {
		System.out.println("Official registry:\n========================");
		printAll("http://centerregistry-clarin.esc.rzg.mpg.de/restxml/");

		System.out.println("Testing registry:\n========================");
		printAll("https://centres-staging.clarin.eu:4430/restxml/");
	}

	@Test
	public void testEq() throws NamingException {
                Environment env = RULE.getEnvironment();
		try {
			Set<Endpoint> list1, list2;
			{
				String centerRegistryUrl = "http://centerregistry-clarin.esc.rzg.mpg.de/restxml/";
				CenterRegistry centerRegistry = new CenterRegistryLive(centerRegistryUrl, null, env);
				list1 = new HashSet<Endpoint>();
				for (Institution i : centerRegistry.getCQLInstitutions()) {
					list1.addAll(i.getEndpoints());
				}
			}

			{
				String centerRegistryUrl = "https://centres-staging.clarin.eu:4430/restxml/";
				CenterRegistry centerRegistry = new CenterRegistryLive(centerRegistryUrl, null, env);
				list2 = new HashSet<Endpoint>();
				for (Institution i : centerRegistry.getCQLInstitutions()) {
					list2.addAll(i.getEndpoints());
				}
			}

			Assert.assertTrue(list1.size() > 10);
			Assert.assertTrue(list2.size() > 10);

			for (Endpoint e : list1) {
				Assert.assertTrue("testing registry does not contain " + e, list2.contains(e));
			}
			for (Endpoint e : list2) {
				Assert.assertTrue("official registry does not contain " + e, list1.contains(e));
			}
		} finally {
		}
	}
}
