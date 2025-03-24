package eu.clarin.sru.fcs.aggregator.scan;

import eu.clarin.weblicht.bindings.cmd.StringBinding;
import eu.clarin.weblicht.bindings.cmd.cp.CenterExtendedInformation;
import eu.clarin.weblicht.bindings.cmd.cp.CenterProfile;
import eu.clarin.weblicht.bindings.cmd.cp.WebReference;
import eu.clarin.weblicht.bindings.cr.Center;
import eu.clarin.weblicht.connectors.ConnectorException;
import eu.clarin.weblicht.connectors.cr.CenterRegistryConnector;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.client.Client;
import org.slf4j.LoggerFactory;

public class CenterRegistryLive implements CenterRegistry {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(CenterRegistryLive.class);

    public static final Integer CONNECT_TIMEOUT = 3000;
    public static final Integer READ_TIMEOUT = 10000;

    private String centerRegistryUrl;
    private boolean hasInstitutionsLoaded = false;
    private List<Institution> centers = new ArrayList<Institution>();
    private final EndpointFilter filter;
    private final Client client;

    public CenterRegistryLive(String centerRegistryUrl, EndpointFilter filter, Client jerseyClient) {
        super();
        this.centerRegistryUrl = centerRegistryUrl;
        this.filter = filter;
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
                CenterExtendedInformation info = profile.getCenterExtendedInformation();

                String institutionUrl = info.getWebsite();
                String institutionName = regCenter.getCenterName();
                Institution institution = new Institution(institutionName, institutionUrl);

                List<WebReference> webRefs = info.getWebReference();
                if (webRefs != null) {
                    for (WebReference webRef : webRefs) {
                        List<StringBinding> sbs = webRef.getDescription();
                        for (StringBinding sb : sbs) {
                            if ("CQL".equals(sb.getValue())) {
                                String endpoint = webRef.getWebsite();
                                if (filter == null || filter.filter(endpoint)) {
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
