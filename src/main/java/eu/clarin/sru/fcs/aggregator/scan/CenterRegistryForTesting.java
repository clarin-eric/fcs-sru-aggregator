package eu.clarin.sru.fcs.aggregator.scan;

import java.util.ArrayList;
import java.util.List;
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
public class CenterRegistryForTesting implements CenterRegistry {

    private static final Logger logger = Logger.getLogger(CenterRegistryForTesting.class.getName());
    private boolean hasChildrenLoaded = false;
	private List<Institution> centers = new ArrayList<Institution>() {
		{
			Institution inst = new Institution("test_IDS", null);
			inst.addEndpoint("https://clarin.ids-mannheim.de/digibibsru-new");
			add(inst);
		}
	};

    @Override
    public boolean hasCQLInstitutionsLoaded() {
		return true;
    }

    @Override
    public void loadCQLInstitutions() {
    }

    @Override
    public List<Institution> getCQLInstitutions() {
        return centers;
    }

    @Override
    public Institution getCQLInstitution(int index) {
        if (index >= centers.size()) {
            return null;
        }
        return centers.get(index);
    }
}
