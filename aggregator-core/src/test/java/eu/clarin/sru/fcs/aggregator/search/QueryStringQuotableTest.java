package eu.clarin.sru.fcs.aggregator.search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testing silent correction of non-quoted query strings to quoted by
 * Aggregator.
 *
 * @author ljo
 */
public class QueryStringQuotableTest {

    @Test
    public void testWSQuotable() {
        final String qWS = "grüne Autos";
        final String queryString = Search.quoteIfQuotableExpression(qWS, "cql");
        assertEquals('"', queryString.charAt(0));
    }

    @Test
    public void testAnglesQuotable() {
        final String qAngles = "<häs>hesitationintended";
        final String queryString = Search.quoteIfQuotableExpression(qAngles, "cql");
        assertEquals('"', queryString.charAt(0));
        assertTrue(queryString.length() == 25);
    }

    @Test
    public void testParensQuotable() {
        final String qParens = "((pausintended";
        final String queryString = Search.quoteIfQuotableExpression(qParens, "cql");
        assertEquals('"', queryString.charAt(0));
        assertTrue(queryString.length() == 16);
    }

    @Test
    public void testEqualsQuotable() {
        final String qEq = "grüne=Autos";
        final String queryString = Search.quoteIfQuotableExpression(qEq, "cql");
        assertEquals('"', queryString.charAt(0));
        assertTrue(queryString.length() == 13);
    }

    @Test
    public void testSlashQuotable() {
        final String qEq = "Echt//";
        final String queryString = Search.quoteIfQuotableExpression(qEq, "cql");
        assertEquals('"', queryString.charAt(0));
        assertTrue(queryString.length() == 8);
    }

    @Test
    public void testNotQuotable() {
        final String qNonQ = "Atomisch";
        final String queryString = Search.quoteIfQuotableExpression(qNonQ, "cql");
        assertEquals('A', queryString.charAt(0));
        assertTrue(queryString.length() == 8);
    }

    @Test
    public void testNotQuotableFCS() {
        final String qNonQ = "[word = 'Atomisch']";
        final String queryString = Search.quoteIfQuotableExpression(qNonQ, "fcs");
        assertEquals('[', queryString.charAt(0));
        assertTrue(queryString.length() == 19);
    }

}
