package eu.clarin.sru.fcs.aggregator.app;

import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.LoggerFactory;

import de.mpg.aai.shhaa.config.ConfigContext;
import de.mpg.aai.shhaa.config.ConfigContextListener;
import de.mpg.aai.shhaa.config.ConfigurationException;

public class AuthConfigContextListener implements ServletContextListener {
    private org.slf4j.Logger log = LoggerFactory.getLogger(ConfigContextListener.class);

    public final static String PARAM_NAME = "ShhaaConfigLocation";

    private ConfigContext configCtx;

    public AuthConfigContextListener() {
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        this.closeConfigContext(event.getServletContext());
    }

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext servletCtx = event.getServletContext();
        this.initConfigContext(servletCtx);
    }

    public void initConfigContext(ServletContext servletCtx) {
        log.debug("init configuration context");
        String location = null;

        try {
            if (ConfigContext.getActiveConfigContext(servletCtx) != null) {
                throw new IllegalStateException(
                        "cannot initialize context because there is already a root application context present - check whether you have multiple ConfigContext* definitions in your web.xml");
            } else {
                location = servletCtx.getInitParameter(PARAM_NAME);
                if (location == null) {
                    location = "/WEB-INF/shhaa.xml";
                    log.debug("no config-location found as init-parameter {}, fallback to {}", PARAM_NAME,
                            location);
                } else {
                    log.debug("found config-location from init-parameter {}: {}", PARAM_NAME, location);
                }

                URL locURL = location.startsWith("/")
                        ? io.dropwizard.util.Resources.getResource(location.substring(1))
                        : new URL(location);
                ConfigContext ctx = new ConfigContext();
                ctx.init(locURL);
                this.configCtx = ctx;
                servletCtx.setAttribute(ConfigContext.CONTEXT_ID, this.configCtx);
            }
        } catch (ConfigurationException cex) {
            log.error("failed to initialize configuration context: {}", cex.getMessage());
            throw cex;
        } catch (MalformedURLException muex) {
            throw new ConfigurationException("invalid configuration file location " + location, muex);
        }
    }

    
    public void closeConfigContext(ServletContext servletCtx) {
        log.debug("closing configuration context");
        this.configCtx = null;
        servletCtx.removeAttribute(ConfigContext.CONTEXT_ID);
    }

}
