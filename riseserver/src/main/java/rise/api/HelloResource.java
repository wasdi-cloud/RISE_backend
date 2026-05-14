package rise.api;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import rise.lib.business.Organization;
import rise.lib.config.RiseConfig;
import rise.lib.utils.Utils;
import rise.lib.utils.log.RiseLog;
import rise.lib.utils.mail.MailUtils;
import rise.lib.viewmodels.ContactMessageViewModel;


/**
 * Root resource (exposed at "hello" path)
 */
@Path("hello")
public class HelloResource {
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String hello(@QueryParam("name") String sName) {
    	
    	RiseLog.debugLog("HelloResource.hello");
    	
    	String sReturn = "";
    	
    	if (!Utils.isNullOrEmpty(sName)) {
    		sReturn += "Hello " + sName + "\n";
    	}
    	    	
        return sReturn + "Welcome to RISE 1.0!";
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
    
    @POST
    @Path("contact")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response contact(ContactMessageViewModel oMessage) {
    	try {
    		RiseLog.debugLog("HelloResource.contact");
    		
    		if (oMessage == null) {
    			return Response.status(Status.BAD_REQUEST).build();
    		}
    		
    		String sMailSubject = "Message from RISE Web Site - " + oMessage.name + " " + oMessage.surname;
    		
    		String sMailText = "From: " + oMessage.name + " " + oMessage.surname + "\n";
    		sMailText += "Address: " + oMessage.email + "\n";
    		
    		if (Utils.isNullOrEmpty(oMessage.company)) oMessage.company = "NULL";
    		sMailText += "Company: " + oMessage.company + "\n";
    		
    		if (Utils.isNullOrEmpty(oMessage.role)) oMessage.role = "NULL";
    		sMailText += "Role: " + oMessage.role + "\n";
    		
    		if (Utils.isNullOrEmpty(oMessage.heardAbout)) oMessage.heardAbout = "NULL";
    		sMailText += "Heard About us: " + oMessage.heardAbout + "\n";

    		if (Utils.isNullOrEmpty(oMessage.subject)) oMessage.subject = "NULL";
    		sMailText += "Subject: " + oMessage.subject + "\n";
    		
    		sMailText += "\n-------------------------------------------------------\n\n";
    		sMailText += oMessage.message;
    		
    		if (MailUtils.sendEmail(RiseConfig.Current.notifications.wasdiAdminMail, sMailSubject, sMailText)) {
    			return Response.ok().build();	
    		}
    		else {
    			RiseLog.errorLog("HelloResource.contact: there was an error sending the email");
    			return Response.serverError().build();
    		}
    		
    		
    		
    	}
    	catch (Exception oEx) {
			RiseLog.errorLog("HelloResource.contact: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
    	
    }
    
}
