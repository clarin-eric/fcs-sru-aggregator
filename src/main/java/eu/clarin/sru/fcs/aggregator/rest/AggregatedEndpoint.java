package eu.clarin.sru.fcs.aggregator.rest;

import java.util.logging.Logger;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author yanapanchenko
 */
@Path("/aggregated-endpoint")
public class AggregatedEndpoint {

    private static final Logger LOGGER = Logger.getLogger(AggregatedEndpoint.class.getName());

    public AggregatedEndpoint() {
        super();
    }

    @GET
    @Path("/scan-report")
    @Produces(MediaType.TEXT_PLAIN)
    public String getScanReport() {
        return "scan-report TODO";
    }

    @GET
    @Path("/scan-report")
    @Produces(MediaType.TEXT_HTML)
    public String getScanReport2() {
        return "<HTML>\n"
                + "   <HEAD>\n"
                + "      <TITLE>\n"
                + "         scan report\n"
                + "      </TITLE>\n"
                + "   </HEAD>\n"
                + "<BODY>\n"
                + "   <P>scan-report TODO</P>\n"
                + "</BODY>\n"
                + "</HTML>";
    }
}
