package eu.clarin.sru.fcs.aggregator.scan.centre_registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import eu.clarin.sru.fcs.aggregator.scan.centre_registry.pojo.Centre;
import eu.clarin.sru.fcs.aggregator.scan.centre_registry.pojo.Consortium;
import eu.clarin.sru.fcs.aggregator.scan.centre_registry.pojo.FCSEndpoint;

public class CenterRegistry {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(CenterRegistry.class);

    public static final Integer CONNECT_TIMEOUT = 3000;
    public static final Integer READ_TIMEOUT = 10000;

    protected final Client client;
    protected final String baseUri;
    protected final JacksonJsonProvider clientJsonMapper;

    protected final EndpointFilter filter;

    public CenterRegistry(Client client, String baseUri) {
        this(client, baseUri, null);
    }

    public CenterRegistry(Client client, String baseUri, EndpointFilter filter) {
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
        List<FCSEndpoint> endpoints = retrieveFCSEndpoints();
        if (endpoints == null || endpoints.isEmpty()) {
            return null;
        }

        List<Centre> centres = retrieveCentres();
        if (centres == null || centres.isEmpty()) {
            return null;
        }

        List<Consortium> consortia = retrieveConsortia();

        // merge together the various data objects
        Map<Integer, Institution> institutionLookup = new HashMap<>();
        for (final FCSEndpoint fcsEndpoint : endpoints) {
            if (filter != null && !filter.filter(fcsEndpoint.getFields().getUri())) {
                continue;
            }

            final int centreId = fcsEndpoint.getFields().getCentreId();
            if (!institutionLookup.containsKey(centreId)) {
                final Centre centre = centres.stream().filter(c -> c.getId() == centreId).findFirst().orElseThrow();

                Optional<Consortium> consortium = Optional.empty();
                if (consortia != null && !consortia.isEmpty()) {
                    consortium = consortia.stream().filter(c -> c.getId() == centre.getFields().getConsortiumId())
                            .findFirst();
                }
                final String consortiumName = consortium.map(Consortium::getFields)
                        .map(Consortium.ConsortiumFields::getName).orElse(null);
                if (filter != null && !filter.filterConsortium(consortiumName)) {
                    continue;
                }

                final Institution institution = new Institution(centre.getFields().getName(),
                        centre.getFields().getWebsiteUrl(),
                        consortiumName);
                institutionLookup.put(centreId, institution);
            }
            Institution institution = institutionLookup.get(centreId);
            institution.addEndpoint(fcsEndpoint.getFields().getUri());
        }

        return new ArrayList<>(institutionLookup.values());
    }

    protected List<FCSEndpoint> retrieveFCSEndpoints() {
        try {
            return client
                    .target(baseUri)
                    .register(clientJsonMapper)
                    .path("api")
                    .path("model")
                    .path("FCSEndpoint")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .get()
                    .readEntity(new GenericType<List<FCSEndpoint>>() {
                    });
        } catch (ProcessingException e) {
            log.error("Error processing FCSEndpoint request", e);
        }
        return null;
    }

    protected List<Centre> retrieveCentres() {
        try {
            return client
                    .target(baseUri)
                    .register(clientJsonMapper)
                    .path("api")
                    .path("model")
                    .path("Centre")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .get()
                    .readEntity(new GenericType<List<Centre>>() {
                    });
        } catch (ProcessingException e) {
            log.error("Error processing Centre request", e);
        }
        return null;
    }

    protected List<Consortium> retrieveConsortia() {
        try {
            return client
                    .target(baseUri)
                    .register(clientJsonMapper)
                    .path("api")
                    .path("model")
                    .path("Consortium")
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .get()
                    .readEntity(new GenericType<List<Consortium>>() {
                    });
        } catch (ProcessingException e) {
            log.error("Error processing Consortium request", e);
        }
        return null;
    }

}
