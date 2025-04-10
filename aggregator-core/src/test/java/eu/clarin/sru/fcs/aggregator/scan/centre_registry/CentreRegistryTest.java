package eu.clarin.sru.fcs.aggregator.scan.centre_registry;

import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import eu.clarin.sru.fcs.aggregator.scan.Institution;
import eu.clarin.sru.fcs.aggregator.scan.centre_registry.pojo.Centre;
import eu.clarin.sru.fcs.aggregator.scan.centre_registry.pojo.Consortium;
import eu.clarin.sru.fcs.aggregator.scan.centre_registry.pojo.FCSEndpoint;

@Disabled("Live tests only manually")
public class CentreRegistryTest {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(CentreRegistryTest.class);

    static final String centreRegistryUrl = "https://centres.clarin.eu/";

    static Client client;
    static CenterRegistry cr;

    @BeforeAll
    static void initAll() {
        client = ClientBuilder.newClient();
        cr = new CenterRegistry(client, centreRegistryUrl);
    }

    @AfterAll
    static void tearDownAll() {
        client.close();
    }

    @Test
    public void testGetFCSEndpoints() {
        List<FCSEndpoint> endpoints = cr.retrieveFCSEndpoints();
        log.info("Got {} endpoints", endpoints.size());
    }

    @Test
    public void testGetCentres() {
        List<Centre> centres = cr.retrieveCentres();
        log.info("Got {} centres", centres.size());
    }

    @Test
    public void testGetConsortiums() {
        List<Consortium> consortiums = cr.retrieveConsortia();
        log.info("Got {} consortiums", consortiums.size());
    }

    @Test
    public void testRetrieveInstitutionsWithFCSEndpoints() {
        List<Institution> institutions = cr.retrieveInstitutionsWithFCSEndpoints();
        log.info("Got {} institutions", institutions.size());
    }

    static {
        org.apache.log4j.BasicConfigurator
                .configure(new org.apache.log4j.ConsoleAppender(
                        new org.apache.log4j.PatternLayout("%-5p [%c{1}][%t] %m%n"),
                        org.apache.log4j.ConsoleAppender.SYSTEM_ERR));
        org.apache.log4j.Logger logger = org.apache.log4j.Logger.getRootLogger();
        logger.setLevel(org.apache.log4j.Level.INFO);
        logger.getLoggerRepository().getLogger("eu.clarin").setLevel(org.apache.log4j.Level.DEBUG);
    }
}
