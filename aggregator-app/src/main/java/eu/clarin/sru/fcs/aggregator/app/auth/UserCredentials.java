package eu.clarin.sru.fcs.aggregator.app.auth;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        String result = getFirstValue(userPrincipal, List.of("displayName", "commonName", "cn"));
        if (result == null) {
            result = getPrincipalName();
        }
        return result;
    }

    public String getOrganization() {
        return getFirstValue(userPrincipal, List.of("organizationname", "organizationName", "o"));
    }

    // ----------------------------------------------------------------------
    // this should be automatic by using the shhaa.xml configuration
    // - Principal.getName() (de.mpg.aai.shhaa.model.AuthPrincipal)
    // - de.mpg.aai.shhaa.config.Configuration.getShibUsernameIDs()
    // - authentication > shibheader > username

    public String getEduPersonPrincipalName() {
        return getFirstValue(userPrincipal, List.of("oid-edupersonprincipalname", "oid-eduPersonPrincipalName",
                "mace-eduPersonPrincipalName", "eduPersonPrincipalName", "oid-eppn", "mace-eppn", "eppn"));
    }

    public String getEduPersonTargetedID() {
        return getFirstValue(userPrincipal, List.of("edupersontargetedid", "oid-eduPersonTargetedID",
                "mace-eduPersonTargetedID", "eduPersonTargetedID"));
    }

    public String getEmail() {
        return getFirstValue(userPrincipal, List.of("mail"));
    }

    public List<String> getEmails() {
        return getFirstValues(userPrincipal, List.of("mail"));
    }

    public String getUserID() {
        // should be equal to getPrincipalName() / userPrincipal.getName()
        String result = null;
        if (result == null) {
            result = getEduPersonPrincipalName();
        }
        if (result == null) {
            result = getEduPersonTargetedID();
        }
        if (result == null) {
            result = getEmail();
        }
        return result;
    }

    /* DEBUG */
    public Map<String, List<String>> getAllAttributes() {
        Map<String, List<String>> result = new HashMap<>();
        if (userPrincipal instanceof AuthPrincipal) {
            AuthPrincipal authPrincipal = (AuthPrincipal) userPrincipal;
            AuthAttributes attributes = authPrincipal.getAttribues();
            for (String key : attributes.getIDs()) {
                List<String> values = getValues(authPrincipal, key);
                result.put(key, values);
            }
        }
        return result;
    }

    // ----------------------------------------------------------------------

    private static String getValue(AuthPrincipal authPrincipal, String key) {
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

    private static List<String> getValues(AuthPrincipal authPrincipal, String key) {
        List<String> result = new ArrayList<>();
        AuthAttributes attributes = authPrincipal.getAttribues();
        if (attributes != null) {
            AuthAttribute<?> authAttribute = attributes.get(key);
            if (authAttribute != null) {
                Set<?> authAttrValues = authAttribute.getValues();
                if (authAttrValues != null) {
                    for (Object authAttrValue : authAttrValues) {
                        if (authAttrValue instanceof String) {
                            result.add((String) authAttrValue);
                        }
                    }
                }
            }
        }
        return result;
    }

    private static String getFirstValue(Principal userPrincipal, List<String> attributes) {
        String result = null;
        if (userPrincipal instanceof AuthPrincipal) {
            AuthPrincipal authPrincipal = (AuthPrincipal) userPrincipal;
            for (String key : attributes) {
                result = getValue(authPrincipal, key);
                if (result != null) {
                    break;
                }
            }
        }
        return result;
    }

    private static List<String> getFirstValues(Principal userPrincipal, List<String> attributes) {
        List<String> result = new ArrayList<>();
        if (userPrincipal instanceof AuthPrincipal) {
            AuthPrincipal authPrincipal = (AuthPrincipal) userPrincipal;
            for (String key : attributes) {
                result = getValues(authPrincipal, key);
                if (result != null && !result.isEmpty()) {
                    break;
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