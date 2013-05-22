/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.clarin.sru.fcs.aggregator.sparam2;

import java.util.Comparator;
import org.zkoss.zul.DefaultTreeNode;

/**
 *
 * @author Yana Panchenko <yana_panchenko at yahoo.com>
 */
public class CorpusByNameDComparator implements Comparator<DefaultTreeNode<Corpus2>> {

    @Override
    public int compare(DefaultTreeNode<Corpus2> o1, DefaultTreeNode<Corpus2> o2) {
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
