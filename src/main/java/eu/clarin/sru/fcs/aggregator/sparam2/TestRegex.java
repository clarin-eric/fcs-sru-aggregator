/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.clarin.sru.fcs.aggregator.sparam2;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Yana Panchenko <yana_panchenko at yahoo.com>
 */
public class TestRegex {
    
    public static void main(String[] args) {
        Pattern p = Pattern.compile(".*[<>=/()\\s].*"); // < > = / ( ) and whitespace
        Matcher m = p.matcher("a/a");
        boolean b = m.matches();
        System.out.println(b);
    }

}
