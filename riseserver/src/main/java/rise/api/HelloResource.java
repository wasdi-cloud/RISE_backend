package rise.api;

import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import rise.lib.business.Organization;
import rise.lib.config.RiseConfig;
import rise.lib.utils.RunTimeUtils;
import rise.lib.utils.ShellExecReturn;
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
    	    	
        return sReturn + "Welcome to RISE 0.1!";
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
    
    @GET
    @Path("pythonScript")
    @Produces(MediaType.APPLICATION_JSON)
    public Response launchPythonScript(@QueryParam("pythonScriptPath") String sPythonScript) {
    	
    	try {
        	List<String> asArgs = new ArrayList<String>();
        	String sScriptsPath = RiseConfig.Current.paths.scriptsPath;
        	if (!sScriptsPath.endsWith("/")) sScriptsPath += "/";
        	asArgs.add(sScriptsPath+sPythonScript);
        	ShellExecReturn oReturn = RunTimeUtils.shellExec(asArgs, true, true);
        	System.out.println(oReturn.getOperationLogs());
        	System.out.println(oReturn.getOperationReturn());
        	return Response.ok().build();    		
    	}
    	catch (Exception oEx) {
    		RiseLog.errorLog("HelloResource.launchPythonScript: exception " + oEx.toString());
    		return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
    }
}
