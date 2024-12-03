package eu.clarin.sru.fcs.aggregator.rest;

import eu.clarin.sru.fcs.aggregator.app.UserCredentials;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthenticationInfo {

    @JsonProperty
    private boolean authenticated;
    @JsonProperty
    private String username;
    @JsonProperty
    private String displayName;
    @JsonProperty
    private String organization;
    @JsonProperty
    private String userId;
    @JsonProperty
    private Map<String, List<String>> attributes;

    public AuthenticationInfo() {
    }

    public AuthenticationInfo(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public AuthenticationInfo(UserCredentials userInfo) {
        authenticated = (userInfo != null);
        if (userInfo != null) {
            username = userInfo.getPrincipalName();
            displayName = userInfo.getDisplayName(); // ignore, maybe for frontend as indicator?
            organization = userInfo.getOrganization();
            userId = userInfo.getUserID();

            // WIP: for now only for debugging, to retrieve all available SAML attributes
            attributes = userInfo.getAllAttributes();
        }
    }

    public static AuthenticationInfo fromPrincipal(final Principal userPrincipal) {
        if (userPrincipal == null) {
            return new AuthenticationInfo(false);
        }

        if (userPrincipal.getName() == null || userPrincipal.getName().isEmpty() || userPrincipal.getName().isBlank()
                || userPrincipal.getName().equals("anonymous")) {
            return new AuthenticationInfo(false);
        }

        final UserCredentials credentials = new UserCredentials(userPrincipal);
        return new AuthenticationInfo(credentials);

    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getOrganization() {
        return organization;
    }

    public String getUserId() {
        return userId;
    }

    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public String toString() {
        return String.format("User name: [%s], display name: [%s], organization: [%s], isAuthenticated: [%b]", username,
                displayName, organization, authenticated);
    }

}