package eu.clarin.sru.fcs.aggregator.app;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpMethod;
import org.slf4j.LoggerFactory;

/**
 * The Aggregator is a single web page application, and most of the URLs
 * are routed and handled by the javascript client, not in the server.
 * 
 * Until jetty 9.4.33.v20201020 we could "abuse" an ErrorHandler, so
 * whenever a 404 pops up in the server, it probably meant that the URL
 * should be handled by javascript, and to do that we must return the
 * normal index.html HTML code.
 * See aggregator version < 3.4.0, ErrorHandler by edima/ljo
 * 
 * Now, we add a filter to all requests and if it is a POST request to
 * the application root ("/", "index.html") then we save all form
 * parameters in the session and forward to the normal index.html.
 *
 * Sometimes the server gets POST calls from external apps (VLO, for
 * example). We must keep the special parameters which are sent to the
 * Aggregator and pass them to the js client, and, again, serve the
 * normal index.html file.
 * 
 * @author koerner
 */
public class ExternalSearchRequestForwardingFilter implements Filter {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ExternalSearchRequestForwardingFilter.class);

    public static final String PARAM_QUERY = "query";
    public static final String PARAM_MODE = "mode";
    public static final String PARAM_AGGREGATION_CONTEXT = "x-aggregation-context";

    public static final String redirectRoute = "/";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if ((request instanceof HttpServletRequest) && (response instanceof HttpServletResponse)) {
            HttpServletRequest req = (HttpServletRequest) request;
            HttpServletResponse resp = (HttpServletResponse) response;
            log.debug("path={} method={}", req.getPathInfo(), req.getMethod());
            log.debug("request={}", req);
            if (req.getMethod().equals("POST") && req.getPathInfo() == "/") {
                // an external search request, coming from clarin.eu or VLO
                log.info("Received external search request");
                saveParameters(req);
                if (forward(req, resp, redirectRoute)) {
                    return;
                }
            }
        }

        chain.doFilter(request, response); // This signals that the request should pass this filter
    }

    private void saveParameters(HttpServletRequest request) {
        {
            String[] queryValues = request.getParameterValues(PARAM_QUERY);
            if (queryValues != null && queryValues.length > 0) {
                request.getSession().setAttribute(PARAM_QUERY, queryValues[0]);
            }
        }

        {
            String[] modeValues = request.getParameterValues(PARAM_MODE);
            if (modeValues != null && modeValues.length > 0) {
                request.getSession().setAttribute(PARAM_MODE, modeValues[0]);
            }
        }

        {
            String[] contextValues = request.getParameterValues(PARAM_AGGREGATION_CONTEXT);
            if (contextValues != null && contextValues.length > 0) {
                request.getSession().setAttribute(PARAM_AGGREGATION_CONTEXT, contextValues[0]);
                request.setAttribute(PARAM_AGGREGATION_CONTEXT, contextValues[0]);
                log.info("request.contextValues > 0: {}", contextValues[0]);
            }
        }
    }

    private boolean forward(HttpServletRequest request, HttpServletResponse response, final String target)
            throws IOException {
        ServletContext ctx = request.getServletContext();
        RequestDispatcher dispatcher = ctx.getNamedDispatcher("static");
        if (dispatcher == null) {
            log.error("Can not find internal redirect route '{}'. Will default.", target);
            return false;
        }

        StringBuilder params = new StringBuilder();
        if (request.getSession().getAttribute(PARAM_AGGREGATION_CONTEXT) != null) {
            params.append("x-aggregation-context=" + request.getSession().getAttribute(PARAM_AGGREGATION_CONTEXT));
        }

        request = new HttpServletRequestWrapper(request) {
            @Override
            public String getMethod() {
                return HttpMethod.GET.asString();
            }

            @Override
            public String getPathInfo() {
                return target;
            }
        };

        request.setAttribute(RequestDispatcher.FORWARD_QUERY_STRING, params.toString());
        request.getSession().setAttribute(RequestDispatcher.FORWARD_QUERY_STRING, params.toString());
        try {
            log.debug("Dispatching attribute check: {} {}",
                    request.getSession().getAttribute(PARAM_AGGREGATION_CONTEXT), request.getSession());
            response.reset();
            dispatcher.forward(request, response);
        } catch (ServletException e) {
            log.debug("Dispatching failed, attribute check: {}",
                    request.getSession().getAttribute(PARAM_AGGREGATION_CONTEXT));
            return false;
        }

        return true;
    }

}
