package eu.clarin.sru.fcs.aggregator.scan;

//import com.sun.jersey.api.client.Client;
import eu.clarin.weblicht.bindings.cmd.StringBinding;
import eu.clarin.weblicht.bindings.cmd.cp.CenterExtendedInformation;
import eu.clarin.weblicht.bindings.cmd.cp.CenterProfile;
import eu.clarin.weblicht.bindings.cmd.cp.WebReference;
import eu.clarin.weblicht.bindings.cr.Center;
import eu.clarin.weblicht.connectors.ConnectorException;
import eu.clarin.weblicht.connectors.cr.CenterRegistryConnector;
import io.dropwizard.setup.Environment;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.client.Client;

public class CenterRegistryLive implements CenterRegistry {

    private static final Logger LOGGER = Logger.getLogger(CenterRegistryLive.class.getName());

    private static final Integer CONNECT_TIMEOUT = 3000;
    private static final Integer READ_TIMEOUT = 10000;

    private String centerRegistryUrl;
    private boolean hasInstitutionsLoaded = false;
    private List<Institution> centers = new ArrayList<Institution>();
    private final EndpointFilter filter;
    private final Client client;

    public CenterRegistryLive(String centerRegistryUrl, EndpointFilter filter, Environment env) {
        super();
        this.centerRegistryUrl = centerRegistryUrl;
        this.filter = filter;
        this.client = ClientFactory.create(CONNECT_TIMEOUT, READ_TIMEOUT, env);
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
        try (CenterRegistryConnector connector = new CenterRegistryConnector(client, url)) {
            List<Center> regCenters = connector.retrieveCenters();
            for (Center regCenter : regCenters) {
                String institutionUrl = regCenter.getId();
                String institutionName = regCenter.getCenterName();
                Institution institution = new Institution(institutionName, institutionUrl);
                // display in the tree only those institutions that have CQL endpoints:
                CenterProfile profile = connector.retrieveCenterProfile(regCenter);
                CenterExtendedInformation info = profile.getCenterExtendedInformation();
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
            Logger.getLogger(CenterRegistryLive.class.getName()).log(Level.SEVERE, null, ex);
        }
        LOGGER.log(Level.FINE, "Number of Centers: {0}", centers.size());
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
