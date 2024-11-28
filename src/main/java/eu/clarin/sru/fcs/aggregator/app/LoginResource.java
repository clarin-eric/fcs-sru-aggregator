package eu.clarin.sru.fcs.aggregator.app;

import java.net.URI;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.slf4j.LoggerFactory;

@Path("/")
public class LoginResource {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(LoginResource.class);

    @POST
    @Path("login")
    public Response postLogin(@QueryParam("redirect") @DefaultValue("") String redirectUri, @Context UriInfo uriInfo,
            @Context final SecurityContext security) {
        log.debug("Client has triggered authentication request [POST]: '{}' -> '{}'. Redirect URI: '{}'.",
                security != null ? security.getUserPrincipal() : null, uriInfo.getRequestUri(), redirectUri);

        if (redirectUri == null || redirectUri.isBlank()) {
            log.debug("redirect to '/' path");
            return Response.seeOther(URI.create("/")).build();
        } else {
            log.debug("redirect to: {}", redirectUri);
            return Response.seeOther(URI.create(redirectUri)).build();
        }
    }

    @GET
    @Path("login")
    public Response getLogin(@QueryParam("redirect") @DefaultValue("") String redirectUri, @Context UriInfo uriInfo,
            @Context final SecurityContext security) {
        log.debug("Client has triggered authentication request [GET]: '{}' -> '{}'. Redirect URI: '{}'.",
                security != null ? security.getUserPrincipal() : null, uriInfo.getRequestUri(), redirectUri);

        if (redirectUri == null || redirectUri.isBlank()) {
            log.debug("redirect to '/' path");
            return Response.seeOther(URI.create("/")).build();
        } else {
            log.debug("redirect to: {}", redirectUri);
            return Response.seeOther(URI.create(redirectUri)).build();
        }
    }

}
