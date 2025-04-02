package eu.clarin.sru.fcs.aggregator.app.auth;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

import org.slf4j.LoggerFactory;

import de.mpg.aai.shhaa.HttpAuthService;
import de.mpg.aai.shhaa.config.ConfigContext;
import de.mpg.aai.shhaa.config.ConfigLoader;
import de.mpg.aai.shhaa.config.Configuration;
import de.mpg.aai.shhaa.config.ConfigurationException;
import de.mpg.aai.shhaa.config.ServiceLoader;

public class AuthConfigContext extends ConfigContext {
    private static org.slf4j.Logger log = LoggerFactory.getLogger(AuthConfigContext.class);

    private String shibWebappHost;
    private String shibLogin;
    private String shibLogout;

    public AuthConfigContext(String shibWebappHost, String shibLogin, String shibLogout) {
        super();
        this.shibWebappHost = shibWebappHost;
        this.shibLogin = shibLogin;
        this.shibLogout = shibLogout;
    }

    @Override
    public void reload() {
        log.debug("(re)loading configuration, from location {}", this.getLocation());
        if (this.getLocation() == null) {
            throw new ConfigurationException("no config file location specified - call init(URL/String) first");
        } else {
            try {
                // this.config = ConfigLoader.load(this);
                final Configuration config = ConfigLoader.load(this);
                Field configField = this.getClass().getSuperclass().getDeclaredField("config");
                configField.setAccessible(true);
                configField.set(this, config);

                // do our custom overriding ...
                if (shibWebappHost != null && !shibWebappHost.isBlank()) {
                    // remove any trailing slashes if there are any
                    if (shibWebappHost.endsWith("/")) {
                        shibWebappHost = shibWebappHost.replaceFirst("/*$", "");
                    }
                    config.setHost(shibWebappHost);
                }
                if (shibLogin != null && !shibLogin.isBlank()) {
                    config.setSSO(shibLogin, Optional.ofNullable(config.getSsoAction()).orElse("lI"));
                }
                if (shibLogout != null && !shibLogout.isBlank()) {
                    config.setSLO(shibLogout, Optional.ofNullable(config.getSloAction()).orElse("lO"));
                }

                // this.authSrv = ServiceLoader.load(this);
                Method loadMethod = ServiceLoader.class.getDeclaredMethod("load", ConfigContext.class);
                loadMethod.setAccessible(true);
                final HttpAuthService authSrv = (HttpAuthService) loadMethod.invoke(null, this);
                Field authSrvField = this.getClass().getSuperclass().getDeclaredField("authSrv");
                authSrvField.setAccessible(true);
                authSrvField.set(this, authSrv);
            } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException
                    | NoSuchMethodException | InvocationTargetException e) {
                log.error("Failure doing extremely hacking configuration loading for AAI ...", e);
                throw new ConfigurationException("Hacky configuration update failed", e);
            }
        }
    }
}
