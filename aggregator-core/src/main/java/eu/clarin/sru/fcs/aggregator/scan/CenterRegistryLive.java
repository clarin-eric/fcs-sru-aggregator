package eu.clarin.sru.fcs.aggregator.scan;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.client.Client;

import org.slf4j.LoggerFactory;

import eu.clarin.weblicht.bindings.cmd.StringBinding;
import eu.clarin.weblicht.bindings.cmd.cp.CenterBasicInformation;
import eu.clarin.weblicht.bindings.cmd.cp.CenterExtendedInformation;
import eu.clarin.weblicht.bindings.cmd.cp.CenterProfile;
import eu.clarin.weblicht.bindings.cmd.cp.Country;
import eu.clarin.weblicht.bindings.cmd.cp.WebReference;
import eu.clarin.weblicht.bindings.cr.Center;
import eu.clarin.weblicht.connectors.ConnectorException;
import eu.clarin.weblicht.connectors.cr.CenterRegistryConnector;

public class CenterRegistryLive implements CenterRegistry {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(CenterRegistryLive.class);

    public static final Integer CONNECT_TIMEOUT = 3000;
    public static final Integer READ_TIMEOUT = 10000;

    private String centerRegistryUrl;
    private boolean hasInstitutionsLoaded = false;
    private List<Institution> centers = new ArrayList<Institution>();
    private final EndpointFilter endpointFilter;
    private final CentreFilter centreFilter;
    private final Client client;

    public CenterRegistryLive(String centerRegistryUrl, Client jerseyClient) {
        this(centerRegistryUrl, null, null, jerseyClient);
    }

    public CenterRegistryLive(String centerRegistryUrl, EndpointFilter endpointFilter, Client jerseyClient) {
        this(centerRegistryUrl, endpointFilter, null, jerseyClient);
    }

    public CenterRegistryLive(String centerRegistryUrl, EndpointFilter endpointFilter, CentreFilter centreFilter,
            Client jerseyClient) {
        super();
        this.centerRegistryUrl = centerRegistryUrl;
        this.endpointFilter = endpointFilter;
        this.centreFilter = centreFilter;
        this.client = jerseyClient;
    }

    @Override
    public boolean hasCQLInstitutionsLoaded() {
        return hasInstitutionsLoaded;
    }

    @Override
    public void loadCQLInstitutions() {
        if (hasInstitutionsLoaded) {
            return;
        }
        hasInstitutionsLoaded = true;
        URI url = URI.create(centerRegistryUrl);
        try (CenterRegistryConnector connector = new CenterRegistryConnector(client, url) {
            @Override
            public void close() {
                // NOTE: ignore closing of client in CenterRegistryConnector->AbstractConnector
                // TODO: better to close and create a new client?
                // But would require resetting dropwizard metrics
            };
        }) {
            List<Center> regCenters = connector.retrieveCenters();
            for (Center regCenter : regCenters) {
                // display in the tree only those institutions that have CQL endpoints:
                CenterProfile profile = connector.retrieveCenterProfile(regCenter);
                if (centreFilter != null && !centreFilter.filter(profile)) {
                    continue;
                }

                CenterBasicInformation infoBa = profile.getCenterBasicInformation();
                String countryCode = Optional.of(infoBa)
                        .map(CenterBasicInformation::getCountry)
                        .map(Country::getCode)
                        .map(codes -> codes.stream()
                                .map(c -> c.getValue()).filter(c -> c != null)
                                .map(c -> c.value())
                                .findFirst().orElse(null))
                        .orElse(null);

                CenterExtendedInformation infoEx = profile.getCenterExtendedInformation();
                String institutionUrl = infoEx.getWebsite();
                String institutionName = regCenter.getCenterName();

                Institution institution = new Institution(institutionName, institutionUrl, countryCode);

                List<WebReference> webRefs = infoEx.getWebReference();
                if (webRefs != null) {
                    for (WebReference webRef : webRefs) {
                        List<StringBinding> sbs = webRef.getDescription();
                        for (StringBinding sb : sbs) {
                            if ("CQL".equals(sb.getValue())) {
                                String endpoint = webRef.getWebsite();
                                if (endpointFilter == null || endpointFilter.filter(endpoint)) {
                                    institution.addEndpoint(endpoint);
                                }
                                break;
                            }
                        }
                    }
                }
                if (!institution.getEndpoints().isEmpty()) {
                    centers.add(institution);
                }
            }

        } catch (ConnectorException ex) {
            log.error("Exception loading CQL institutions:", ex);
        }
        log.debug("Number of Centers: {}", centers.size());
    }

    @Override
    public List<Institution> getCQLInstitutions() {
        loadCQLInstitutions();
        return centers;
    }

    @Override
    public Institution getCQLInstitution(int index) {
        loadCQLInstitutions();
        if (index >= centers.size()) {
            return null;
        }
        return centers.get(index);
    }
}
