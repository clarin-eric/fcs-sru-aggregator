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
public class CorpusByInstitutionComparator implements Comparator<DefaultTreeNode<Corpus2>> {

    @Override
    public int compare(DefaultTreeNode<Corpus2> o1, DefaultTreeNode<Corpus2> o2) {
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
