package eu.clarin.sru.fcs.aggregator.sopt;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Center registry node. Its children are centers (institutions).
 *
 * @author Yana Panchenko
 */
public class CenterRegistryForTesting implements CenterRegistryI {

    private static final Logger logger = Logger.getLogger(CenterRegistryForTesting.class.getName());
    private boolean hasChildrenLoaded = false;
    private List<InstitutionI> centers = new ArrayList<InstitutionI>();
    private static final String[] INSTITUTION_URLS = new String[]{
        "http://130.183.206.32/restxml/5"
    };
    private static final String[] INSTITUTION_NAMES = new String[]{
        "MPI"
    };
    private static final String[][] INSTITUTION_ENDPOINTS = new String[][]{
        new String[]{ // MPI endpoints, contact person Olha 
            "http://lux17.mpi.nl/cqltest"
        }
    };

    @Override
    public boolean hasCQLInstitutionsLoaded() {
        return hasChildrenLoaded;
    }

    @Override
    public void loadCQLInstitutions() {
        if (hasChildrenLoaded) {
            return;
        }
        hasChildrenLoaded = true;
        loadCQLInstitutionsForTesting();
        logger.log(Level.FINE, "Number of Centers: {0}", centers.size());
    }

    @Override
    public List<InstitutionI> getCQLInstitutions() {
        loadCQLInstitutions();
        return centers;
    }

    @Override
    public InstitutionI getCQLInstitution(int index) {
        loadCQLInstitutions();
        if (index >= centers.size()) {
            return null;
        }
        return centers.get(index);
    }

    private void loadCQLInstitutionsForTesting() {
        for (int i = 0; i < INSTITUTION_ENDPOINTS.length; i++) {
            InstitutionI institution = new Institution(INSTITUTION_NAMES[i], INSTITUTION_URLS[i]);
            for (int j = 0; j < INSTITUTION_ENDPOINTS.length; j++) {
                institution.add(INSTITUTION_ENDPOINTS[i][j]);
            } 
            if (!institution.getEndpoints().isEmpty()) {
                centers.add(institution);
            }
        }
    }
}
