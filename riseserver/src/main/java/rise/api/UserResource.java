package rise.api;

import java.util.Date;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import rise.lib.business.Session;
import rise.lib.business.User;
import rise.lib.data.SessionRepository;
import rise.lib.data.UserRepository;
import rise.lib.utils.Utils;
import rise.lib.utils.date.DateUtils;
import rise.lib.utils.log.RiseLog;
import rise.lib.viewmodels.RegisterViewModel;
import rise.lib.viewmodels.RiseViewModel;
import rise.lib.viewmodels.SessionTokenViewModel;
import rise.lib.viewmodels.UserCredentialsViewModel;

@Path("user")
public class UserResource {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("login")
    public Response login(UserCredentialsViewModel oUserCredentialsVM) {
    	
    	try {
			// Validate inputs
			if (oUserCredentialsVM == null) {
				RiseLog.warnLog("UserResource.login: login info null, user not authenticated");
				return Response.status(Status.BAD_REQUEST).build();
			}
			if(Utils.isNullOrEmpty(oUserCredentialsVM.userId)){
				RiseLog.warnLog("UserResource.login: userId null or empty, user not authenticated");
				return Response.status(Status.BAD_REQUEST).build();	
			}
			if(Utils.isNullOrEmpty(oUserCredentialsVM.password)){
				RiseLog.warnLog("UserResource.login: password null or empty, user not authenticated");
				return Response.status(Status.BAD_REQUEST).build();
			}
			
	    	RiseLog.debugLog("UserResource.login");
	    	
	    	UserRepository oUserRepository =  new UserRepository();
	    	User oUser = oUserRepository.getUser(oUserCredentialsVM.userId);
	    	
	    	if (oUser == null) {
	    		return Response.status(Status.UNAUTHORIZED).build();
	    	}
	    	
	    	
	    	
	    	Session oSession = new Session();
	    	
	    	oSession.setLoginDate(DateUtils.getDateAsDouble(new Date()));
	    	oSession.setLastTouch(DateUtils.getDateAsDouble(new Date()));
	    	oSession.setToken(Utils.getRandomName());
	    	oSession.setUserId(oUserCredentialsVM.userId);
	    	
	    	SessionRepository oSessionRepository = new SessionRepository();
	    	oSessionRepository.add(oSession);
	    	
	    	SessionTokenViewModel oSessionTokenViewModel = (SessionTokenViewModel) RiseViewModel.getFromEntity(SessionTokenViewModel.class.getName(), oSession);
	    	
	    	return Response.ok(oSessionTokenViewModel).build();
    	}
		catch (Exception oEx) {
			RiseLog.errorLog("UserResource.login: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("register")
    public Response register(RegisterViewModel oRegisterVM) {
    	try {
    		
    		if (oRegisterVM == null) {
				RiseLog.warnLog("UserResource.register: register info null");
				return Response.status(Status.BAD_REQUEST).build();
    		}
    		
    		if (oRegisterVM.admin == null) {
				RiseLog.warnLog("UserResource.register: register admin null");
				return Response.status(Status.BAD_REQUEST).build();
    		}
    		
    		if (oRegisterVM.organization == null) {
				RiseLog.warnLog("UserResource.register: register organization null");
				return Response.status(Status.BAD_REQUEST).build();
    		}
    		
    		if (oRegisterVM.organization.name == null) {
				RiseLog.warnLog("UserResource.register: register organization name null");
				return Response.status(Status.BAD_REQUEST).build();
    		}    		
    		
    		if (oRegisterVM.admin.userId == null) {
				RiseLog.warnLog("UserResource.register: register admin id null");
				return Response.status(Status.BAD_REQUEST).build();
    		}
    		
    		if (oRegisterVM.admin.userId == null) {
				RiseLog.warnLog("UserResource.register: register admin id null");
				return Response.status(Status.BAD_REQUEST).build();
    		}
    		
    		if (oRegisterVM.password == null) {
				RiseLog.warnLog("UserResource.register: register password null");
				return Response.status(Status.BAD_REQUEST).build();
    		}    		
    		

    		
    		return Response.ok().build();
    	}
		catch (Exception oEx) {
			RiseLog.errorLog("UserResource.register: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
    }
    
    

}
