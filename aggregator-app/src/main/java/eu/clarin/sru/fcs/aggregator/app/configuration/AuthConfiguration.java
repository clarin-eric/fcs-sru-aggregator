package eu.clarin.sru.fcs.aggregator.app.configuration;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import eu.clarin.sru.fcs.aggregator.app.util.IgnoreNotNullIfNotEnabled;
import eu.clarin.sru.fcs.aggregator.app.util.PEMKeyStringDeserializer;

@IgnoreNotNullIfNotEnabled
public class AuthConfiguration {

    public static class KeysConfig {

        @JsonProperty
        private String publicKeyFile;

        @JsonProperty
        private String privateKeyFile;

        @JsonProperty
        @JsonDeserialize(using = PEMKeyStringDeserializer.class)
        String publicKey;

        // TODO: better masking with
        // https://stackoverflow.com/questions/56070451/mask-json-fields-using-jackson
        @JsonProperty(access = Access.WRITE_ONLY)
        @JsonDeserialize(using = PEMKeyStringDeserializer.class)
        String privateKey;

        // ------------------------------------------------------------------

        @JsonProperty("publicKeyFile")
        public String getPublicKeyFile() {
            return publicKeyFile;
        }

        @JsonProperty("privateKeyFile")
        public String getPrivateKeyFile() {
            return privateKeyFile;
        }

        @JsonProperty("publicKey")
        public String getPublicKey() {
            return publicKey;
        }

        @JsonProperty("privateKey")
        public String getPrivateKey() {
            return privateKey;
        }

    }

    // ----------------------------------------------------------------------

    @JsonProperty
    private boolean enabled = false;

    @IgnoreNotNullIfNotEnabled.IgnorableNotNull
    @Valid
    @JsonProperty
    private KeysConfig keys;

    @IgnoreNotNullIfNotEnabled.IgnorableNotNull
    @JsonProperty
    private String shibWebappHost;

    @IgnoreNotNullIfNotEnabled.IgnorableNotNull
    @JsonProperty
    private String shibLogin;

    @IgnoreNotNullIfNotEnabled.IgnorableNotNull
    @JsonProperty
    private String shibLogout;

    // ----------------------------------------------------------------------

    @JsonProperty("enabled")
    public boolean isEnabled() {
        return enabled;
    }

    @JsonProperty("keys")
    public KeysConfig getKeys() {
        return keys;
    }

    @JsonProperty("shibWebappHost")
    public String getShibWebappHost() {
        return shibWebappHost;
    }

    @JsonProperty("shibLogin")
    public String getShibLogin() {
        return shibLogin;
    }

    @JsonProperty("shibLogout")
    public String getShibLogout() {
        return shibLogout;
    }

}
