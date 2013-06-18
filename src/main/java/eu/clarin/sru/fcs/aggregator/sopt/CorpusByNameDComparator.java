package eu.clarin.sru.fcs.aggregator.sopt;

import java.util.Comparator;
import org.zkoss.zul.DefaultTreeNode;

/**
 *
 * @author Yana Panchenko
 */
public class CorpusByNameDComparator implements Comparator<DefaultTreeNode<Corpus>> {

    @Override
    public int compare(DefaultTreeNode<Corpus> o1, DefaultTreeNode<Corpus> o2) {
        String name1 = "~";
        String name2 = "~";
        if (o1.getData().getDisplayName() != null) {
            name1 = o1.getData().getDisplayName();
        }
        if (o2.getData().getDisplayName() != null) {
            name2 = o2.getData().getDisplayName();
        }
        return name2.compareToIgnoreCase(name1);
    }

}
