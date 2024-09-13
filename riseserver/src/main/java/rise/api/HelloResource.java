package rise.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import rise.lib.business.Organization;
import rise.lib.utils.Utils;
import rise.lib.utils.log.RiseLog;

/**
 * Root resource (exposed at "hello" path)
 */
@Path("hello")
public class HelloResource {
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String hello(@QueryParam("name") String sName) {
    	
    	RiseLog.debugLog("Hello.hello");
    	
    	String sReturn = "";
    	
    	if (!Utils.isNullOrEmpty(sName)) {
    		sReturn += "Hello " + sName + "\n";
    	}
    	    	
        return sReturn + "Welcome to RISE!";
    }
    
    @GET
    @Path("json")
    @Produces(MediaType.APPLICATION_JSON)
    public Organization jsonTest() {
    	Organization oOrganization = new Organization();
    	
    	oOrganization.setName("WASDI Sarl");
    	oOrganization.setCity("Dudelange");
    	
    	return oOrganization;
    }
}
