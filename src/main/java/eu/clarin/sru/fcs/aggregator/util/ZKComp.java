package eu.clarin.sru.fcs.aggregator.util;

import org.zkoss.zul.A;

/**
 * Utility for reusable ZK components created for Aggregator.
 * 
 * @author Yana Panchenko <yana.panchenko@uni-tuebingen.de>
 */
public class ZKComp {

    public static A createCorpusHomeLink(String landingPage) {
            A link = new A();
            link.setTarget("_blank");
            link.setHref(landingPage);
            link.setImage("img/go-home.png");
            return link;
    }
    
}
