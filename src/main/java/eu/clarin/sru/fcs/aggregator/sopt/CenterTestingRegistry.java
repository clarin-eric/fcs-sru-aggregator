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
public class CenterTestingRegistry implements StartingPointFCS {

    private static final Logger logger = Logger.getLogger(CenterTestingRegistry.class.getName());
    private boolean hasChildrenLoaded = false;
    private List<Institution> centers = new ArrayList<Institution>();
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
    public boolean hasInstitutionsLoaded() {
        return hasChildrenLoaded;
    }

    @Override
    public void loadInstitutions() {
        if (hasChildrenLoaded) {
            return;
        }
        hasChildrenLoaded = true;
        loadInstitutionsForTesting();
        logger.log(Level.FINE, "Number of Centers: {0}", centers.size());
    }

    @Override
    public List<Institution> getInstitutions() {
        loadInstitutions();
        return centers;
    }

    @Override
    public Institution getInstitution(int index) {
        loadInstitutions();
        if (index >= centers.size()) {
            return null;
        }
        return centers.get(index);
    }

    private void loadInstitutionsForTesting() {
        for (int i = 0; i < INSTITUTION_ENDPOINTS.length; i++) {
            Institution institution = new InstitutionForTesting(INSTITUTION_NAMES[i], INSTITUTION_URLS[i], INSTITUTION_ENDPOINTS[i]);
            if (!institution.getChildren().isEmpty()) {
                centers.add(institution);
            }
        }
    }
}
