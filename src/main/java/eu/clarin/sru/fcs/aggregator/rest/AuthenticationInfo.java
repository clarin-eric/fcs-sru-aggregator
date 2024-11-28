package eu.clarin.sru.fcs.aggregator.rest;

import eu.clarin.sru.fcs.aggregator.app.UserCredentials;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;

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
    private List<String> userId;

    public AuthenticationInfo() {
    }

    public AuthenticationInfo(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public AuthenticationInfo(UserCredentials userInfo) {
        this.authenticated = (userInfo != null);
        if (userInfo != null) {
            this.username = userInfo.getPrincipalName();
            this.displayName = userInfo.getDisplayName(); // ignore, maybe for frontend as indicator?
            this.organization = userInfo.getOrganization();
            // WIP: for now only for debugging, to list the chain of attributes that are
            // checked to retrieve the username
            this.userId = Arrays.asList(new String[] { userInfo.getEmail(), userInfo.getEduPersonPrincipalName(),
                    userInfo.getEduPersonTargetedID(), userInfo.getUserID() });
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

    public List<String> getUserId() {
        return userId;
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