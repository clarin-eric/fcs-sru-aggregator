package eu.clarin.sru.fcs.aggregator.core;

public interface FCSAuthenticationParams {
    boolean enableAAI();

    String getServerUrl();

    String getPublicKey();

    String getPrivateKey();

    String getPublicKeyFile();

    String getPrivateKeyFile();
}
