package eu.clarin.sru.fcs.aggregator.scan.textplus_registry;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import eu.clarin.sru.fcs.aggregator.scan.EndpointFilter;
import eu.clarin.sru.fcs.aggregator.scan.EndpointUrlFilterAllow;
import eu.clarin.sru.fcs.aggregator.scan.textplus_registry.pojo.Institution;
import eu.clarin.sru.fcs.aggregator.scan.textplus_registry.pojo.InstitutionResponse;
import eu.clarin.sru.fcs.aggregator.scan.textplus_registry.pojo.RepositoryListResponse;
import eu.clarin.sru.fcs.aggregator.scan.textplus_registry.pojo.Response;

@Disabled
public class TextplusRegistryTest {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(TextplusRegistryTest.class);

    static final String tpRegistryUrl = "https://registry.text-plus.org/";

    static Client client;
    static TextplusRegistry tr;

    @BeforeAll
    static void initAll() {
        client = ClientBuilder.newClient();

        EndpointFilter filter = new EndpointUrlFilterAllow("fedora.clarin-d.uni-saarland.de");
        filter = null; // disable filter
        tr = new TextplusRegistry(client, tpRegistryUrl, filter);
    }

    @AfterAll
    static void tearDownAll() {
        client.close();
    }

    @Test
    public void testRetrieveInstitution() {
        String entityId = "39f56810-40fa-48c8-add7-ac860ab30e02";
        InstitutionResponse institution = tr.retrieveInstitution(entityId);
        log.info("Got {}", institution);
    }

    @Test
    public void testRetrieveGeneric() {
        String url = "https://registry.text-plus.org/admin/api/v1/e/f6c1bdc4-e83a-4eaa-a347-0f970a204aac";
        Response<Institution> item = tr.retrieveGeneric(url, new GenericType<>() {
        });
        log.info("Got {}", item);
        if (item.getItem().getProperties() != null) {
            log.info("n: {}", item.getItem().getProperties().getName());
        }
    }

    @Test
    public void testRetrieveRepositories() {
        RepositoryListResponse repositories = tr.retrieveRepositories();
        log.info("Got {} repositories", repositories.getSize());

        repositories.getItems().stream()
                .forEach(r -> {
                    log.info("e: {} / u: {}", r.getEntityId(), r.getUniqueId());
                    log.info(" * n: {}", r.getProperties().getPrimaryName());
                    if (r.getProperties().getInstitution() != null) {
                        r.getProperties().getInstitution().stream()
                                .forEach(i -> {
                                    final String institutionId = i.getInstitution().getReference();
                                    log.info(" -> i: {}", institutionId);

                                    InstitutionResponse institution = tr.retrieveInstitution(institutionId);
                                    log.info("     * n: {}", institution.getItem().getProperties().getName());
                                });
                    }
                    if (r.getProperties().getUriIriFcs() != null) {
                        r.getProperties().getUriIriFcs().stream()
                                .forEach(u -> log.info(" - {}", u));
                    }

                });

    }

    @Test
    public void testRetrieveInstitutionsWithFCSEndpoints() {
        List<eu.clarin.sru.fcs.aggregator.scan.Institution> institutions = tr.retrieveInstitutionsWithFCSEndpoints();
        log.info("Got {} institutions", institutions.size());
        log.info("institutions: {}", institutions);
        log.info("endpoints: {}", institutions.stream()
                .map(i -> i.getEndpoints()).flatMap(e -> e.stream())
                .collect(Collectors.toList()));
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
