package rise.filters;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.container.ContainerRequestContext;

/**
 * Standard CORS Filter: it adds CORS headers to all the call
 * @author p.campanella
 *
 */
public class CORSFilter implements Filter {
	
	/**
	 * Clean the filter
	 */
	@Override
	public void destroy() {
		
	}
	
	
	/**
	 * Apply the filter
	 */
	@Override
	public void doFilter(ServletRequest oServletRequest, ServletResponse oServletResponse, FilterChain oFilterChain) throws IOException, ServletException {
		if (oServletResponse instanceof HttpServletResponse) {
            HttpServletResponse oAlteredResponse = ((HttpServletResponse) oServletResponse);
            addHeadersFor200Response(oAlteredResponse);
        }
		
		
        oFilterChain.doFilter(oServletRequest, oServletResponse);
	}
	
	/**
	 * Adds CORS headers
	 * @param oResponse
	 */
	private void addHeadersFor200Response(HttpServletResponse oResponse) {
        oResponse.addHeader("Access-Control-Allow-Origin", "*");
        oResponse.addHeader("Access-Control-Allow-Methods", "GET, PUT, POST, OPTIONS, DELETE, X-XSRF-TOKEN");
        oResponse.addHeader("Access-Control-Allow-Headers", "Cache-Control, Pragma, Origin, Authorization, content-type, X-Requested-With, accept, x-session-token, x-refdate");
        oResponse.addHeader("Access-Control-Allow-Credentials", "true");
    }
	
	/**
	 * Init filter
	 */
	@Override
	public void init(FilterConfig arg0) throws ServletException {
	}
	

}
