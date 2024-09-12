package rise.filters;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

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
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		if (servletResponse instanceof HttpServletResponse) {
            HttpServletResponse alteredResponse = ((HttpServletResponse) servletResponse);
            addHeadersFor200Response(alteredResponse);
        }
		
		
        filterChain.doFilter(servletRequest, servletResponse);
	}
	
	/**
	 * Adds CORS headers
	 * @param response
	 */
	private void addHeadersFor200Response(HttpServletResponse response) {
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "GET, PUT, POST, OPTIONS,DELETE, X-XSRF-TOKEN");
        response.addHeader("Access-Control-Allow-Headers", "Cache-Control, Pragma, Origin, Authorization, content-type, X-Requested-With, accept, x-session-token, x-refdate");
        response.addHeader("Access-Control-Allow-Credentials", "true");
    }
	
	/**
	 * Init filter
	 */
	@Override
	public void init(FilterConfig arg0) throws ServletException {
		
	}
	

}
