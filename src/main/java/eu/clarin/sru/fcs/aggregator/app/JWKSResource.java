package eu.clarin.sru.fcs.aggregator.app;

import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

import eu.clarin.sru.client.auth.KeyReaderUtils;

@Path("/.well-known")
public class JWKSResource {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(JWKSResource.class);

    /**
     * JSON Web Key Set
     * 
     * @see https://datatracker.ietf.org/doc/html/rfc7517#section-5
     */
    public static class JWKS {

        /**
         * JSON Web Key
         * 
         * @see https://datatracker.ietf.org/doc/html/rfc7517
         * @see https://www.scottbrady91.com/jose/jwts-which-signing-algorithm-should-i-use
         */
        public static class JWK {
            @JsonProperty("kty")
            public String keyType = "RSA";

            @JsonProperty("use")
            public String usage = "sig";

            // @JsonProperty("alg")
            // public String algorithm = "RS256";

            @JsonProperty("kid")
            public String keyId;

            // @JsonProperty("key_ops")
            // public List<String> keyOperations = List.of("verify");

            // @JsonProperty("x5c")
            // public String x509CertificateChain;

            // @JsonProperty("x5t")
            // public String x509Thumbprint;

            @JsonProperty("e")
            public String exponent;

            @JsonProperty("n")
            public String modulus;

            protected JWK withKid(String keyId) {
                this.keyId = keyId;
                return this;
            }

            protected JWK from(RSAPublicKey key) {
                keyType = key.getAlgorithm(); // "RSA"

                // TODO: really unsure whether this is even correct
                // NOTE: key size =/= key algorithm
                // algorithm = "RS" + (key.getModulus().bitLength() / 8);

                exponent = toB64(key.getPublicExponent());
                modulus = toB64(key.getModulus());

                return this;
            }

            public static JWK create() {
                return new JWK();
            }

            /**
             * JSON Web Signature (JWS) compliant Base64Url encoding (without padding).
             * 
             * @param value bytes to be base64url encoded.
             * @return base64 encoded bytes
             * @see https://datatracker.ietf.org/doc/html/rfc7515#appendix-C
             */
            private static String toB64(byte[] value) {
                return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
            }

            /**
             * JSON Web Signature (JWS) compliant Base64Url encoding (without padding).
             * 
             * @param value BigInteger value, to be converted to bytes and then base64url
             *              encoded.
             * @return base64 encoded bytes of BigInteger value.
             * @see #toB64(byte[])
             * @see https://stackoverflow.com/questions/4407779/biginteger-to-byte
             */
            private static String toB64(BigInteger value) {
                byte[] bValue = value.toByteArray();
                if (bValue[0] == 0) {
                    bValue = Arrays.copyOfRange(bValue, 1, bValue.length);
                }
                return toB64(bValue);
            }
        }

        @JsonProperty(value = "keys", required = true)
        public List<JWK> keys = new ArrayList<>();

        public JWKS withKey(JWK jwk) {
            keys.add(jwk);
            return this;
        }
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Path("jwks.json")
    public Response getJWKS() {
        AggregatorConfiguration.Params.AAIConfig aaiConfig = Aggregator.getInstance().getParams().aaiConfig;
        if (aaiConfig == null || !aaiConfig.enabled) {
            return Response.status(Status.NOT_FOUND).entity("No authentication support enabled at server!").build();
        }

        RSAPublicKey publicKey = null;
        try {
            String publicKeyContent = aaiConfig.keys.publicKey;
            publicKey = KeyReaderUtils.readPublicKey(publicKeyContent);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
            log.error("Failed to load public key, no JWKS possible.", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error loading key!").build();
        }

        return Response.ok(
                new JWKS().withKey(
                        JWKS.JWK.create()
                                .from(publicKey)
                                .withKid("jwt")))
                .build();
    }
}
