package eu.clarin.sru.fcs.aggregator.app;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ext.Provider;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;

/**
 *
 * @author edima
 */
@Provider
public class ErrorPageHandler extends ErrorPageErrorHandler {

	List<String> staticRoutes;

	public ErrorPageHandler() {
		staticRoutes = new ArrayList<String>();
		staticRoutes.add("/rest");
		staticRoutes.add("/admin");
		staticRoutes.add("/lib");
		staticRoutes.add("/js");
		staticRoutes.add("/fonts");
		staticRoutes.add("/img");
	}

	static boolean prefix(String url, String prefix) {
		return url.equals(prefix) || url.startsWith(prefix + "/") || url.startsWith(prefix + "?");
	}

	@Override
	public String getErrorPage(HttpServletRequest request) {
		String url = request.getPathInfo();
		for (String pre : staticRoutes) {
			if (prefix(url, pre)) {
				return null;
			}
		}
		return "/index.html";
	}
}
