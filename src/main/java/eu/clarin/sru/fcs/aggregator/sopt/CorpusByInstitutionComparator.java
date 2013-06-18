package eu.clarin.sru.fcs.aggregator.sopt;

import java.util.Comparator;
import org.zkoss.zul.DefaultTreeNode;

/**
 *
 * @author Yana Panchenko
 */
public class CorpusByInstitutionComparator implements Comparator<DefaultTreeNode<Corpus>> {

    @Override
    public int compare(DefaultTreeNode<Corpus> o1, DefaultTreeNode<Corpus> o2) {
        String name1 = "~";
        String name2 = "~";
        if (o1.getData().getInstitution() != null && o1.getData().getInstitution().getName() != null) {
            name1 = o1.getData().getInstitution().getName();
        }
        if (o2.getData().getInstitution() != null && o2.getData().getInstitution().getName() != null) {
            name2 = o2.getData().getInstitution().getName();
        }
        return name1.compareToIgnoreCase(name2);
    }

}
