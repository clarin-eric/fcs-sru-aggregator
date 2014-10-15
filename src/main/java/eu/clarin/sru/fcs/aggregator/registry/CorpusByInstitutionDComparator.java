package eu.clarin.sru.fcs.aggregator.registry;

import java.util.Comparator;

/**
 * Comparator necessary for sorting the corpora according to their 
 * institution name in descending order.
 * 
 * @author Yana Panchenko
 */
public class CorpusByInstitutionDComparator implements Comparator<Corpus> {

    @Override
    public int compare(Corpus o1, Corpus o2) {
        String name1 = "~";
        String name2 = "~";
        if (o1.getInstitution() != null && o1.getInstitution().getName() != null) {
            name1 = o1.getInstitution().getName();
        }
        if (o2.getInstitution() != null && o2.getInstitution().getName() != null) {
            name2 = o2.getInstitution().getName();
        }
        return name2.compareToIgnoreCase(name1);
    }

}
