package eu.clarin.sru.fcs.aggregator.app;

import eu.clarin.sru.fcs.aggregator.scan.CenterRegistry;
import eu.clarin.sru.fcs.aggregator.scan.CenterRegistryLive;
import eu.clarin.sru.fcs.aggregator.scan.ClientFactory;
import eu.clarin.sru.fcs.aggregator.scan.Endpoint;
import eu.clarin.sru.fcs.aggregator.scan.Institution;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.naming.NamingException;
import javax.ws.rs.client.Client;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author yanapanchenko
 */
@Disabled("Live tests only manually")
@ExtendWith(DropwizardExtensionsSupport.class)
public class CQLEnumerationTest {

    public static final String CENTER_REGISTRY_OFFICIAL = "https://centres.clarin.eu/restxml/";
    public static final String CENTER_REGISTRY_TESTING = "https://centres-staging.clarin.eu/restxml/";

    @RegisterExtension
    public static final DropwizardAppExtension<AggregatorConfiguration> RULE = new DropwizardAppExtension<>(
            Aggregator.class, ResourceHelpers.resourceFilePath("aggregator_test.yml"));
    // TODO: needs a bit more work, while we create our Aggregator app to access the
    // dropwizard environment, the scan crawler starts and runs in the background
    //
    // NOTE: the dropwizard environment has meters registered, those need to be
    // removed for the center registry jersey client to work, so simply remove all:
    //
    // @formatter:off
    // Environment env = RULE.getEnvironment();
    // env.metrics().removeMatching(new MetricFilter() {
    //     @Override
    //     public boolean matches(String name, Metric metric) {
    //         return name.contains(ClientFactory.class.getName());
    //         // return true;  // to remove all
    //     }
    // });
    // @formatter:on

    public static Client jerseyClient;

    @BeforeAll
    public static void setupJerseyClient() {
        Environment env = RULE.getEnvironment();
        env.metrics().removeMatching(new MetricFilter() {
            @Override
            public boolean matches(String name, Metric metric) {
                return name.contains(ClientFactory.class.getName());
            }
        });
        jerseyClient = ClientFactory.create(CenterRegistryLive.CONNECT_TIMEOUT, CenterRegistryLive.READ_TIMEOUT, env);
    }

    public void printAll(String centerRegistryUrl) throws NamingException {
        try {
            CenterRegistry centerRegistry = new CenterRegistryLive(centerRegistryUrl, null, jerseyClient);
            List<Institution> list = centerRegistry.getCQLInstitutions();
            for (Institution institution : list) {
                System.out.println("1: " + institution.getName() + ": ");
                for (Endpoint e : institution.getEndpoints()) {
                    System.out.println("1: \t -> " + e);
                }
            }
            assertTrue(list.size() > 10);
        } finally {
        }
    }

    @Test
    public void testPrintAll() throws NamingException {
        // CENTER_REGISTRY_URL
        System.out.println("Official registry:\n========================");
        printAll(CENTER_REGISTRY_OFFICIAL);

        System.out.println("Testing registry:\n========================");
        printAll(CENTER_REGISTRY_TESTING);
    }

    @Test
    public void testEq() throws NamingException {
        try {
            Set<Endpoint> list1, list2;
            {
                CenterRegistry centerRegistry = new CenterRegistryLive(CENTER_REGISTRY_OFFICIAL, null, jerseyClient);
                list1 = new HashSet<Endpoint>();
                for (Institution i : centerRegistry.getCQLInstitutions()) {
                    list1.addAll(i.getEndpoints());
                }
            }

            {
                CenterRegistry centerRegistry = new CenterRegistryLive(CENTER_REGISTRY_TESTING, null, jerseyClient);
                list2 = new HashSet<Endpoint>();
                for (Institution i : centerRegistry.getCQLInstitutions()) {
                    list2.addAll(i.getEndpoints());
                }
            }

            assertTrue(list1.size() > 10);
            assertTrue(list2.size() > 10);

            for (Endpoint e : list1) {
                assertTrue(list2.contains(e), "testing registry does not contain " + e);
            }
            for (Endpoint e : list2) {
                assertTrue(list1.contains(e), "official registry does not contain " + e);
            }
        } finally {
        }
    }
}
