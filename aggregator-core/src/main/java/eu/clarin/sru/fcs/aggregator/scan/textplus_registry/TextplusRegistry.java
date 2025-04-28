package eu.clarin.sru.fcs.aggregator.scan.textplus_registry;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJsonProvider;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.clarin.sru.fcs.aggregator.scan.EndpointFilter;
import eu.clarin.sru.fcs.aggregator.scan.Institution;
import eu.clarin.sru.fcs.aggregator.scan.textplus_registry.pojo.Repository.InstitutionRelation;
import eu.clarin.sru.fcs.aggregator.scan.textplus_registry.pojo.InstitutionResponse;
import eu.clarin.sru.fcs.aggregator.scan.textplus_registry.pojo.RepositoryListResponse;
import eu.clarin.sru.fcs.aggregator.scan.textplus_registry.pojo.Response;

public class TextplusRegistry {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(TextplusRegistry.class);

    public static final Integer CONNECT_TIMEOUT = 3000;
    public static final Integer READ_TIMEOUT = 10000;

    protected final Client client;
    protected final String baseUri;
    protected final JacksonJsonProvider clientJsonMapper;

    protected final EndpointFilter filter;

    public TextplusRegistry(Client client, String baseUri) {
        this(client, baseUri, null);
    }

    public TextplusRegistry(Client client, String baseUri, EndpointFilter filter) {
        if (client == null) {
            throw new IllegalArgumentException("client == null");
        }
        if (baseUri == null) {
            throw new IllegalArgumentException("baseUri == null");
        }

        this.client = client;
        this.baseUri = baseUri;
        this.filter = filter;

        // setup client to ignore unknown properties
        this.clientJsonMapper = new JacksonJsonProvider(
                new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false));
    }

    public List<Institution> retrieveInstitutionsWithFCSEndpoints() {
        RepositoryListResponse repositories = retrieveRepositories();

        List<Institution> institutions = repositories.getItems().stream()
                .filter(repo -> repo.getProperties().getUriIriFcs() != null)
                .map(repo -> {
                    String name = repo.getProperties().getPrimaryName();

                    List<InstitutionRelation> repoInstitutions = repo.getProperties().getInstitution();
                    if (repoInstitutions != null) {
                        name = repoInstitutions.stream().map(ri -> {
                            String url = ri.getInstitution().getLinks().get("self");

                            Response<eu.clarin.sru.fcs.aggregator.scan.textplus_registry.pojo.Institution> item = retrieveGeneric(
                                    url, new GenericType<>() {
                                    });

                            return item.getItem().getProperties().getName();
                        }).findAny().orElse(name);
                    }

                    Institution institution = new Institution(name, null);

                    repo.getProperties().getUriIriFcs().stream()
                            // filter endpoints if we have a filter
                            .filter(uri -> filter == null || filter.filter(uri))
                            .forEach(institution::addEndpoint);

                    // NOTE: no filtering of consorita possible (is not valid here)

                    return institution;
                })
                // only keep institutions that have endpoints
                .filter(institution -> !institution.getEndpoints().isEmpty())
                .collect(Collectors.toList());

        return institutions;
    }

    protected RepositoryListResponse retrieveRepositories() {
        try {
            return client
                    .target(baseUri)
                    .register(clientJsonMapper)
                    .path("api")
                    .path("v1")
                    .path("e")
                    .path("repository")
                    .path("/")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .get()
                    .readEntity(RepositoryListResponse.class);
        } catch (ProcessingException e) {
            log.error("Error processing Repository request", e);
        }
        return null;
    }

    protected InstitutionResponse retrieveInstitution(String entityId) {
        try {
            return client
                    .target(baseUri)
                    .register(clientJsonMapper)
                    .path("api")
                    .path("v1")
                    .path("e")
                    .path(entityId)
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .get()
                    .readEntity(InstitutionResponse.class);
        } catch (ProcessingException e) {
            log.error("Error processing Repository request", e);
        }
        return null;
    }

    protected <T> T retrieveGeneric(String url, GenericType<T> entityType) {
        try {
            return client
                    .target(url)
                    .register(clientJsonMapper)
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .get()
                    .readEntity(entityType);
        } catch (ProcessingException e) {
            log.error("Error processing Repository request", e);
        }
        return null;
    }
}
