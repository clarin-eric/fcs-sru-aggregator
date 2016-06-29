package eu.clarin.sru.fcs.aggregator.search;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.Assert;

/**
 * Testing silent correction of non-quoted query strings to quoted by Aggregator.
 * 
 * $Id$
 *
 * @author ljo
 */

public class QueryStringQuotableTest {

    @Test
	public void testWSQuotable() {
	final String qWS = "grüne Autos";
	final String queryString = Search.quoteIfQuotableExpression(qWS, "cql");
	Assert.assertEquals('"', queryString.charAt(0));
    }

    @Test
	public void testAnglesQuotable() {
	final String qAngles = "<häs>hesitationintended";
	final String queryString = Search.quoteIfQuotableExpression(qAngles, "cql");
	Assert.assertEquals('"', queryString.charAt(0));
	Assert.assertTrue(queryString.length() == 25);
    }

    @Test
	public void testParensQuotable() {
	final String qParens = "((pausintended";
	final String queryString = Search.quoteIfQuotableExpression(qParens, "cql");
	Assert.assertEquals('"', queryString.charAt(0));
	Assert.assertTrue(queryString.length() == 16);
    }

    @Test
	public void testEqualsQuotable() {
	final String qEq = "grüne=Autos";
	final String queryString = Search.quoteIfQuotableExpression(qEq, "cql");
	Assert.assertEquals('"', queryString.charAt(0));
	Assert.assertTrue(queryString.length() == 13);
    }

    @Test
	public void testSlashQuotable() {
	final String qEq = "Echt//";
	final String queryString = Search.quoteIfQuotableExpression(qEq, "cql");
	Assert.assertEquals('"', queryString.charAt(0));
	Assert.assertTrue(queryString.length() == 8);
    }

    @Test
	public void testNotQuotable() {
	final String qNonQ = "Atomisch";
	final String queryString = Search.quoteIfQuotableExpression(qNonQ, "cql");
	Assert.assertEquals('A', queryString.charAt(0));
	Assert.assertTrue(queryString.length() == 8);
    }

    @Test
	public void testNotQuotableFCS() {
	final String qNonQ = "[word = 'Atomisch']";
	final String queryString = Search.quoteIfQuotableExpression(qNonQ, "fcs");
	Assert.assertEquals('[', queryString.charAt(0));
	Assert.assertTrue(queryString.length() == 19);
    }

}
