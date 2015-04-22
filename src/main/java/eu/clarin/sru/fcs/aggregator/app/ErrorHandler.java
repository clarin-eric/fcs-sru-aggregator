package eu.clarin.sru.fcs.aggregator.app;

import java.io.IOException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Request;
import org.slf4j.LoggerFactory;

/**
 * @author edima
 *
 * The Aggregator is a single web page application, and most of the URLs are
 * routed and handled by the javascript client, not in the server.
 * Therefore, whenever a 404 pops up in the server, it probably means that the
 * URL should be handled by javascript, and to do that we must return the normal
 * index.html HTML code.
 *
 * Sometimes the server gets POST calls from external apps (VLO, for example).
 * We must keep the special parameters which are sent to the Aggregator and
 * pass them to the js client, and, again, serve the normal index.html file.
 */
public class ErrorHandler extends org.eclipse.jetty.server.handler.ErrorHandler {

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(ErrorHandler.class);

	public static final String PARAM_QUERY = "query";
	public static final String PARAM_MODE = "mode";
	public static final String PARAM_AGGREGATION_CONTEXT = "x-aggregation-context";

	public static final String redirectRoute = "/index.html";


	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
		// On 404 page we need to show index.html and let JS router do the work, otherwise show error page
		if (response.getStatus() == HttpServletResponse.SC_NOT_FOUND) {
			forward(redirectRoute, baseRequest, response);
		} else if (request.getMethod().equals("POST")
				&& response.getStatus() == HttpServletResponse.SC_METHOD_NOT_ALLOWED) {
			// an external search request, coming from clarin.eu or VLO
			{
				String[] queryValues = request.getParameterValues(PARAM_QUERY);
				if (queryValues != null && queryValues.length > 0) {
					baseRequest.getSession().setAttribute(PARAM_QUERY, queryValues[0]);
				}
			}

			{
				String[] modeValues = request.getParameterValues(PARAM_MODE);
				if (modeValues != null && modeValues.length > 0) {
					baseRequest.getSession().setAttribute(PARAM_MODE, modeValues[0]);
				}
			}

			{
				String[] contextValues = request.getParameterValues(PARAM_AGGREGATION_CONTEXT);
				if (contextValues != null && contextValues.length > 0) {
					baseRequest.getSession().setAttribute(PARAM_AGGREGATION_CONTEXT, contextValues[0]);
				}
			}

			baseRequest.setMethod(HttpMethod.GET, HttpMethod.GET.asString());

			forward(redirectRoute, baseRequest, response);
		} else {
			super.handle(target, baseRequest, request, response);
		}
	}

	void forward(String target, Request request, HttpServletResponse response) throws IOException {
		RequestDispatcher dispatcher = request.getRequestDispatcher(target);
		if (dispatcher != null) {
			try {
				response.reset();
				dispatcher.forward(request, response);
			} catch (ServletException e) {
				super.handle(target, request, request, response);
			}
		} else {
			log.error("Can not find internal redirect route '" + target + "' while handling error. Will show system error page");
			super.handle(target, request, request, response);
		}
	}
}
