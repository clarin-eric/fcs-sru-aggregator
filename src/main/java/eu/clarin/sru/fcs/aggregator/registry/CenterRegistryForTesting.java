package eu.clarin.sru.fcs.aggregator.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Center registry node. Its children are centers (institutions). 
 * The class is created after a request from MPI to provide them
 * with a possibility to test their endpoints on development
 * servers with the aggregator before they put them on production
 * server. Institutions and endpoint urls that need to be tested are hard-coded.
 *
 * @author Yana Panchenko
 */
public class CenterRegistryForTesting implements CenterRegistryI {

    private static final Logger logger = Logger.getLogger(CenterRegistryForTesting.class.getName());
    private boolean hasChildrenLoaded = false;
    private List<Institution> centers = new ArrayList<Institution>();
    private static final String[] INSTITUTION_URLS = new String[]{
		"http://130.183.206.32/restxml/3",
		"http://130.183.206.32/restxml/5"
    };
    private static final String[] INSTITUTION_NAMES = new String[]{
		"IDS",
		"MPI"
    };
    private static final String[][] INSTITUTION_ENDPOINTS = new String[][]{
        new String[]{ // MPI endpoints, contact person Olha 
			"https://clarin.phonetik.uni-muenchen.de/BASSRU/",
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

    private void loadCQLInstitutionsForTesting() {
        for (int i = 0; i < INSTITUTION_ENDPOINTS.length; i++) {
            Institution institution = new Institution(INSTITUTION_NAMES[i], INSTITUTION_URLS[i]);
            for (int j = 0; j < INSTITUTION_ENDPOINTS.length; j++) {
				institution.add(INSTITUTION_ENDPOINTS[i][j]);
            } 
			if (!institution.getEndpoints().isEmpty()) {
				centers.add(institution);
			}
        }
    }
}
