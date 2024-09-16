package rise.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import rise.lib.business.Organization;
import rise.lib.business.Session;
import rise.lib.business.User;
import rise.lib.config.RiseConfig;
import rise.lib.data.OrganizationRepository;
import rise.lib.data.SessionRepository;
import rise.lib.data.UserRepository;
import rise.lib.utils.MailUtils;
import rise.lib.utils.PasswordAuthentication;
import rise.lib.utils.Utils;
import rise.lib.utils.date.DateUtils;
import rise.lib.utils.i8n.LangUtils;
import rise.lib.utils.i8n.Languages;
import rise.lib.utils.i8n.StringCodes;
import rise.lib.utils.log.RiseLog;
import rise.lib.viewmodels.ErrorViewModel;
import rise.lib.viewmodels.RegisterViewModel;
import rise.lib.viewmodels.RiseViewModel;
import rise.lib.viewmodels.SessionTokenViewModel;
import rise.lib.viewmodels.UserCredentialsViewModel;

/**
 * User's related APIs
 * 
 */
@Path("user")
public class UserResource {

	/**
	 * Login API
	 * @param oUserCredentialsVM user provided credentials
	 * @return Session View Model with the token of ok, Bad Request otherwise 
	 */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("login")
    public Response login(UserCredentialsViewModel oUserCredentialsVM) {
    	
    	try {
    		
    		ErrorViewModel oErrorViewModel = new ErrorViewModel(StringCodes.ERROR_API_WRONG_USER_OR_PASSWORD.name(), Status.UNAUTHORIZED.getStatusCode());
    		
			// Validate inputs
			if (oUserCredentialsVM == null) {
				RiseLog.warnLog("UserResource.login: login info null, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();
			}
			if(Utils.isNullOrEmpty(oUserCredentialsVM.userId)){
				RiseLog.warnLog("UserResource.login: userId null or empty, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();
			}
			if(Utils.isNullOrEmpty(oUserCredentialsVM.password)){
				RiseLog.warnLog("UserResource.login: password null or empty, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();
			}
	    	
	    	// Check if we have a user
	    	UserRepository oUserRepository =  new UserRepository();
	    	User oUser = oUserRepository.getUser(oUserCredentialsVM.userId);
	    	
	    	if (oUser == null) {
	    		RiseLog.warnLog("UserResource.login: user not found");
	    		return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();
	    	}
	    	
	    	RiseLog.debugLog("UserResource.login");
	    		    	
	    	
	    	// Check if is confirmed
	    	if (oUser.getConfirmationDate() == null) {
	    		RiseLog.warnLog("UserResource.login: user not confirmed yet");
	    		return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();	    		
	    	}
	    	
	    	// really check the password
	    	PasswordAuthentication oPasswordAuthentication = new PasswordAuthentication();
	    	
	    	String sProvidedPw = oUserCredentialsVM.password;
	    	String sEncryptedProvidedPassword = oPasswordAuthentication.hash(sProvidedPw.toCharArray());
	    	if ( ! oUser.getPassword().equals(sEncryptedProvidedPassword)) {
	    		RiseLog.warnLog("UserResource.login: password not valid");
	    		return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();	    		
	    	}
	    	
	    	oUser.setLastLoginDate(DateUtils.getNowAsDouble());
	    	if (!oUserRepository.updateUser(oUser)) {
	    		RiseLog.warnLog("UserResource.login: Error updating users' last login");
	    	}
	    	
	    	double dLastPwChange = oUser.getLastPasswordUpdateDate();
	    	double dNow = DateUtils.getNowAsDouble();
	    	
	    	double dTimePassed = dNow-dLastPwChange;
	    	dTimePassed /= 1000.0;
	    	
	    	if (dTimePassed>RiseConfig.Current.security.maxPasswordAgeSeconds) {
	    		RiseLog.warnLog("UserResource.login: password expired for user " + oUserCredentialsVM.userId);
	    		
	    		oErrorViewModel = new ErrorViewModel(StringCodes.WARNING_API_PASSWORD_EXPIRED.name(), Status.TEMPORARY_REDIRECT.getStatusCode());
	    		
	    		return Response.status(Status.TEMPORARY_REDIRECT).entity(oErrorViewModel).build();
	    	}
	    	
	    	// Create the session for the user
	    	Session oSession = new Session();
	    	
	    	oSession.setLoginDate(DateUtils.getDateAsDouble(new Date()));
	    	oSession.setLastTouch(DateUtils.getDateAsDouble(new Date()));
	    	oSession.setToken(Utils.getRandomName());
	    	oSession.setUserId(oUserCredentialsVM.userId);
	    	
	    	SessionRepository oSessionRepository = new SessionRepository();
	    	oSessionRepository.add(oSession);
	    	
	    	// Save it in the view model
	    	SessionTokenViewModel oSessionTokenViewModel = (SessionTokenViewModel) RiseViewModel.getFromEntity(SessionTokenViewModel.class.getName(), oSession);
	    	
	    	// Return the entity
	    	return Response.ok(oSessionTokenViewModel).build();
    	}
		catch (Exception oEx) {
			RiseLog.errorLog("UserResource.login: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
    }
    
    /**
     * Register a new user/organization
     * @param oRegisterVM Register View Model
     * @return http 200 if ok. ErrorViewModel in case of errors
     */
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
    		
    		if (oRegisterVM.password == null) {
				RiseLog.warnLog("UserResource.register: register password null");
				return Response.status(Status.BAD_REQUEST).build();
    		}
    		
    		PasswordAuthentication oPasswordAuthentication = new PasswordAuthentication();
    		
    		if (!oPasswordAuthentication.isValidPassword(oRegisterVM.password)) {
				RiseLog.warnLog("UserResource.register: password invalid");
				return Response.status(Status.BAD_REQUEST).build();		
    		}
    		
    		// Check if we have a conflict in org or user name
    		boolean bConflict = false;
    		
    		ArrayList<String> asErrors = new ArrayList<>();
    		
    		OrganizationRepository oOrganizationRepository = new OrganizationRepository();
    		
    		// Check if we have organizations with the same name
    		List<Organization> aoSameNameOrganizations = oOrganizationRepository.getOrganizationsByName(oRegisterVM.organization.name);
    		
    		if (aoSameNameOrganizations.size()>0) {
    			RiseLog.errorLog("UserResource.register: there are already organization with this name, impossible to proceed");
    			asErrors.add(StringCodes.ERROR_API_ORG_ALREADY_EXISTS.name());
    			bConflict = true;
    		}
    		
    		// Check if we have an existing user with same user id
    		UserRepository oUserRepository = new UserRepository();
    		
    		User oPotentialExistingUser = oUserRepository.getUser(oRegisterVM.admin.userId);
    		
    		if (oPotentialExistingUser == null) {
    			RiseLog.errorLog("UserResource.register: there are already a user with this name, impossible to proceed");
    			asErrors.add(StringCodes.ERROR_API_ORG_ALREADY_EXISTS.name());
    			bConflict = true;    			
    		}
    		
    		// In case of a conflict, we exit and notify this to the user
    		if (bConflict) {
    			ErrorViewModel oErrorViewModel = new ErrorViewModel(asErrors, Status.CONFLICT.getStatusCode());
    			return Response.status(Status.CONFLICT).entity(oErrorViewModel).build();
    		}
    		
    		if (oRegisterVM.admin.acceptedPrivacy == false || oRegisterVM.admin.acceptedTermsAndConditions == false) {
    			ErrorViewModel oErrorViewModel = new ErrorViewModel(StringCodes.ERROR_API_MISSING_ACCEPT_TERMS_AND_PRIVACY.name(), Status.CONFLICT.getStatusCode());
    			return Response.status(Status.FORBIDDEN).entity(oErrorViewModel).build();    			
    		}
    		
    		// Get now timestamp
    		double dNow = DateUtils.getNowAsDouble();
    		
    		// Assign it to creation date of the org
    		oRegisterVM.organization.creationDate = dNow;
    		
    		// Convert view model in entity
    		Organization oOrganization = (Organization) RiseViewModel.copyToEntity(Organization.class.getName(), oRegisterVM.organization);
    		// Create the id for the org
    		oOrganization.setId(Utils.getRandomName());
    		// Add the org to the repo
    		oOrganizationRepository.add(oOrganization);
    		
    		// Now translate the user
    		User oAdminUser = (User) RiseViewModel.copyToEntity(User.class.getName(), oRegisterVM.admin);
    		
    		// We can now assign the org id
    		oAdminUser.setOrganizationId(oOrganization.getId());
    		
    		// Initialize the dates as now
    		oAdminUser.setRegistrationDate(dNow);
    		oAdminUser.setLastLoginDate(dNow);
    		oAdminUser.setLastPasswordUpdateDate(dNow);
    		oAdminUser.setPrivacyAcceptedDate(dNow);
    		oAdminUser.setTermsAndConditionAcceptedDate(dNow);
    		
    		
    		// Initialize the notifications
    		oAdminUser.setNotifyActivities(true);
    		oAdminUser.setNotifyMaintenance(true);
    		oAdminUser.setNotifyNewsletter(true);
    		
    		// Generate the Confirmation Code
    		String sConfirmationCode = Utils.getRandomName();
    		
    		// Save it
    		oAdminUser.setConfirmationDate(null);
    		oAdminUser.setConfirmationCode(sConfirmationCode);
    		
    		oAdminUser.setPassword(oPasswordAuthentication.hash(oRegisterVM.password.toCharArray()));
    		
    		oUserRepository.updateUser(oAdminUser);
    		
    		// Get localized title and message
    		String sTitle = LangUtils.getLocalizedString(StringCodes.NOTIFICATIONS_ADMIN_CONFIRM_MAIL_TITLE.name() , Languages.EN.name());
    		String sMessage = LangUtils.getLocalizedString(StringCodes.NOTIFICATIONS_ADMIN_CONFIRM_MAIL_MESSAGE.name() , Languages.EN.name());
    		
    		
    		// Generate the confirmation Link
    		String sLink = RiseConfig.Current.serverApiAddress;
    		
    		if (!sLink.endsWith("/")) sLink += "/";
    		sLink += "user/confirmadm?code=" + sConfirmationCode + "&usr=" + oAdminUser.getUserId();
    		
    		// We replace the link in the message
    		sMessage = sMessage.replace("%%LINK%%", sLink);
    		
    		// And we send an email to the user waiting for him to confirm!
    		MailUtils.sendEmail(RiseConfig.Current.notifications.riseAdminMail, oAdminUser.getEmail(), sTitle, sMessage, true);
    		
    		return Response.ok().build();
    	}
		catch (Exception oEx) {
			RiseLog.errorLog("UserResource.register: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
    }
    
    @GET
    @Path("confirmadm")
    public Response confirmAdminUser(@QueryParam("code") String sConfirmationCode, @QueryParam("usr") String sUserId) {
    	try {
    		if (Utils.isNullOrEmpty(sUserId)) {
				RiseLog.warnLog("UserResource.confirmAdminUser: user id null");
				return Response.status(Status.BAD_REQUEST).build();    			
    		}
    		
    		if (Utils.isNullOrEmpty(sConfirmationCode)) {
				RiseLog.warnLog("UserResource.confirmAdminUser: confirmation code null");
				return Response.status(Status.BAD_REQUEST).build();    			
    		}
    		
    		UserRepository oUserRepository = new UserRepository();
    		User oUser = oUserRepository.getUser(sUserId);
    		
    		if (oUser == null) {
				RiseLog.warnLog("UserResource.confirmAdminUser: cannot find user " + sUserId);
				return Response.status(Status.BAD_REQUEST).build();    			    			
    		}
    		
    		if (!oUser.getConfirmationCode().equals(sConfirmationCode)) {
				RiseLog.warnLog("UserResource.confirmAdminUser: wrong confirmation code " + sConfirmationCode);
				return Response.status(Status.BAD_REQUEST).build();    			
    		}
    		
    		double dRegistrationDate = oUser.getRegistrationDate();
    		double dNow = DateUtils.getNowAsDouble();
    		
    		if ((dNow-dRegistrationDate)> ( RiseConfig.Current.security.maxConfirmationAgeSeconds * 1000 ) ) {
				RiseLog.warnLog("UserResource.confirmAdminUser: expired confirmation code " + sConfirmationCode);
				return Response.status(Status.FORBIDDEN).build();    			
    		}
    		
    		oUser.setConfirmationDate(dNow);
    		oUserRepository.updateUser(oUser);
    		
    		return Response.ok().build();
    	}
		catch (Exception oEx) {
			RiseLog.errorLog("UserResource.confirmAdminUser: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
    }
    

}
