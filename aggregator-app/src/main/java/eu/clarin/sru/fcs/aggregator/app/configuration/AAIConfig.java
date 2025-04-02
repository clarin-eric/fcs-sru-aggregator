package eu.clarin.sru.fcs.aggregator.app.configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import eu.clarin.sru.fcs.aggregator.app.util.PEMKeyStringDeserializer;

public final class AAIConfig {
    @JsonProperty
    boolean enabled;

    @JsonProperty
    String shibWebappHost;

    @JsonProperty
    String shibLogin;

    @JsonProperty
    String shibLogout;

    @Valid
    @NotNull
    @JsonProperty
    KeyConfig key;

    // ------------------------------------------------------------------

    @JsonIgnore
    public boolean isAAIEnabled() {
        return enabled;
    }

    @JsonIgnore
    public String getShibWebappHost() {
        return shibWebappHost;
    }

    @JsonIgnore
    public String getShibLogin() {
        return shibLogin;
    }

    @JsonIgnore
    public String getShibLogout() {
        return shibLogout;
    }

    @JsonIgnore
    public KeyConfig getKey() {
        return key;
    }

    // ------------------------------------------------------------------

    // TODO: allow for key filenames

    public static class KeyConfig {
        @JsonProperty
        @JsonDeserialize(using = PEMKeyStringDeserializer.class)
        String publicKey;

        // TODO: better masking with
        // https://stackoverflow.com/questions/56070451/mask-json-fields-using-jackson
        @JsonProperty(access = Access.WRITE_ONLY)
        @JsonDeserialize(using = PEMKeyStringDeserializer.class)
        String privateKey;

        @JsonIgnore
        public String getPublicKey() {
            return publicKey;
        }

        @JsonIgnore
        public String getPrivateKey() {
            return privateKey;
        }

        // TODO: maybe key path properties?
        // TODO: validation?

    }
}
