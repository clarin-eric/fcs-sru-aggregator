package eu.clarin.sru.fcs.aggregator.sopt;

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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Center registry node. Its children are centers (institutions).
 *
 * @author Yana Panchenko
 */
public class CenterRegistryLive implements CenterRegistryI {

    private static final Logger LOGGER = Logger.getLogger(CenterRegistryLive.class.getName());
    private static final String CENTER_REGISTRY_URL = "http://130.183.206.32/restxml/";
    //https://centerregistry-clarin.esc.rzg.mpg.de/restxml/
    private boolean hasInstitutionsLoaded = false;
    private List<Institution> centers = new ArrayList<Institution>();

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
            URI url = URI.create(CENTER_REGISTRY_URL);
            CenterRegistryConnector connector = new CenterRegistryConnector(url, 30000);
             try {
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
                            institution.add(webRef.getWebsite());
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
        } finally {
                 connector.close();
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