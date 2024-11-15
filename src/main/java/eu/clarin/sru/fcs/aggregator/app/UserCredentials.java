package eu.clarin.sru.fcs.aggregator.app;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;

import de.mpg.aai.shhaa.model.AuthAttribute;
import de.mpg.aai.shhaa.model.AuthAttributes;
import de.mpg.aai.shhaa.model.AuthPrincipal;

/**
 * Wrapper class to hold the userPrincipal and a displayName
 *
 * @author jnphilipp
 */
public class UserCredentials {

    private final Principal userPrincipal;

    public UserCredentials(Principal userPrincipal) {
        this.userPrincipal = userPrincipal;
    }

    public Principal getPrincipal() {
        return userPrincipal;
    }

    public String getPrincipalName() {
        return userPrincipal.getName();
    }

    public String getPrincipalNameMD5Hex() {
        return getPrincipalNameMD5Hex(userPrincipal.getName());
    }

    public static String getPrincipalNameMD5Hex(String name) {
        return DigestUtils.md5Hex(name);
    }

    public String getDisplayName() {
        String result = null;
        if (userPrincipal instanceof AuthPrincipal) {
            List<String> displayNamesAttributes = new ArrayList<String>();
            displayNamesAttributes.add("displayName");
            displayNamesAttributes.add("commonName");

            AuthPrincipal authPrincipal = (AuthPrincipal) userPrincipal;
            for (String key : displayNamesAttributes) {
                result = getValue(authPrincipal, key);
                if (result != null) {
                    break;
                }
            }
        }
        if (result == null) {
            result = getPrincipalName();
        }
        return result;
    }

    public String getEmail() {
        String result = null;
        if (userPrincipal instanceof AuthPrincipal) {
            List<String> emailAttributes = new ArrayList<String>();
            emailAttributes.add("email");

            AuthPrincipal authPrincipal = (AuthPrincipal) userPrincipal;
            for (String key : emailAttributes) {
                result = getValue(authPrincipal, key);
                if (result != null) {
                    break;
                }
            }
        }
        if (result == null) {
            result = getPrincipalName();
        }
        return result;
    }

    private String getValue(AuthPrincipal authPrincipal, String key) {
        String result = null;
        AuthAttributes attributes = authPrincipal.getAttribues();
        if (attributes != null) {
            AuthAttribute<?> authAttribute = attributes.get(key);
            if (authAttribute != null) {
                Object authAttrValue = authAttribute.getValue();
                if (authAttrValue instanceof String) {
                    result = (String) authAttrValue;
                }
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return getPrincipal().toString();
    }
}