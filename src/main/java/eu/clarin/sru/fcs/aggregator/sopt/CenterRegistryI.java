package eu.clarin.sru.fcs.aggregator.sopt;

import java.util.List;

/**
 * Interface representing starting point of FCS. For example,
 * center registry, that contains all the centers that in their turn contain
 * the information about supporting endpoints.
 *
 * @author Yana Panchenko
 */
public interface CenterRegistryI {

    public boolean hasCQLInstitutionsLoaded();

    public void loadCQLInstitutions();
        
    public List<InstitutionI> getCQLInstitutions();

    public InstitutionI getCQLInstitution(int index);

}
