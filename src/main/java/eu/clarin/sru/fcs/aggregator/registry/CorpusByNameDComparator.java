package eu.clarin.sru.fcs.aggregator.registry;

import java.util.Comparator;

/**
 * Comparator necessary for sorting the corpora according to the corpus name
 * in descending order.
 * 
 * @author Yana Panchenko
 */
public class CorpusByNameDComparator implements Comparator<Corpus> {

    @Override
    public int compare(Corpus o1, Corpus o2) {
        String name1 = "~";
        String name2 = "~";
        if (o1.getDisplayName() != null) {
            name1 = o1.getDisplayName();
        }
        if (o2.getDisplayName() != null) {
            name2 = o2.getDisplayName();
        }
        return name2.compareToIgnoreCase(name1);
    }

}
