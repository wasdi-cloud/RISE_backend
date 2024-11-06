package rise.api;

import java.net.URLEncoder;
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
import rise.lib.business.OTP;
import rise.lib.business.OTPOperations;
import rise.lib.business.Organization;
import rise.lib.business.Session;
import rise.lib.business.User;
import rise.lib.business.UserRole;
import rise.lib.config.RiseConfig;
import rise.lib.data.OTPRepository;
import rise.lib.data.OrganizationRepository;
import rise.lib.data.SessionRepository;
import rise.lib.data.UserRepository;
import rise.lib.utils.PasswordAuthentication;
import rise.lib.utils.Utils;
import rise.lib.utils.date.DateUtils;
import rise.lib.utils.i8n.LangUtils;
import rise.lib.utils.i8n.Languages;
import rise.lib.utils.i8n.StringCodes;
import rise.lib.utils.log.RiseLog;
import rise.lib.utils.mail.MailUtils;
import rise.lib.viewmodels.ConfirmInviteViewModel;
import rise.lib.viewmodels.ErrorViewModel;
import rise.lib.viewmodels.OTPVerifyViewModel;
import rise.lib.viewmodels.OTPViewModel;
import rise.lib.viewmodels.RegisterViewModel;
import rise.lib.viewmodels.RiseViewModel;
import rise.lib.viewmodels.SessionTokenViewModel;
import rise.lib.viewmodels.UserCredentialsViewModel;

/**
 * User's related APIs
 * 
 */
@Path("auth")
public class AuthResource {

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
				RiseLog.warnLog("AuthResource.login: login info null, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();
			}
			if(Utils.isNullOrEmpty(oUserCredentialsVM.userId)){
				RiseLog.warnLog("AuthResource.login: userId null or empty, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();
			}
			if(Utils.isNullOrEmpty(oUserCredentialsVM.password)){
				RiseLog.warnLog("AuthResource.login: password null or empty, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();
			}
	    	
	    	// Check if we have a user
	    	UserRepository oUserRepository =  new UserRepository();
	    	User oUser = oUserRepository.getUser(oUserCredentialsVM.userId);
	    	
	    	if (oUser == null) {
	    		RiseLog.warnLog("AuthResource.login: user not found");
	    		return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();
	    	}
	    	
	    	RiseLog.debugLog("AuthResource.login");
	    		    	
	    	
	    	// Check if is confirmed
	    	if (oUser.getConfirmationDate() == null) {
	    		RiseLog.warnLog("AuthResource.login: user not confirmed yet");
	    		return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();	    		
	    	}
	    	
	    	// really check the password
	    	PasswordAuthentication oPasswordAuthentication = new PasswordAuthentication();
	    	
	    	String sProvidedPw = oUserCredentialsVM.password;
	    	//String sEncryptedProvidedPassword = oPasswordAuthentication.hash(sProvidedPw.toCharArray());
	    	if ( ! oPasswordAuthentication.authenticate(sProvidedPw.toCharArray(), oUser.getPassword())) {
	    		RiseLog.warnLog("AuthResource.login: password not valid");
	    		return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();	    		
	    	}
	    	
	    	// Create the OTP Entity
	    	OTP oOTP = new OTP();
	    	oOTP.setId(Utils.getRandomName());
	    	oOTP.setSecretCode(Utils.getOTPPassword());
	    	oOTP.setUserId(oUser.getUserId());
	    	oOTP.setValidated(false);
	    	oOTP.setOperation(OTPOperations.LOGIN.name());
	    	oOTP.setTimestamp(DateUtils.getNowAsDouble());
	    	
	    	// Add it to the Db
	    	OTPRepository oOTPRepository = new OTPRepository();
	    	oOTPRepository.add(oOTP);
	    	
	    	RiseLog.debugLog("AuthResource.login: created OTP " + oOTP.getId());
	    	
	    	// Create the view model
	    	OTPViewModel oOTPViewModel = new OTPViewModel();
	    	oOTPViewModel = (OTPViewModel) RiseViewModel.getFromEntity(OTPViewModel.class.getName(), oOTP);
	    	
	    	// Create the verify API address
	    	oOTPViewModel.verifyAPI = RiseConfig.Current.serverApiAddress;
	    	if (!oOTPViewModel.verifyAPI.endsWith("/")) oOTPViewModel.verifyAPI += "/";
	    	oOTPViewModel.verifyAPI += "auth/login_verify";
	    	
    		// Get localized title and message
    		String sTitle = LangUtils.getLocalizedString(StringCodes.OTP_TITLE.name() , Languages.EN.name());
    		String sMessage = LangUtils.getLocalizedString(StringCodes.OTP_MESSAGE.name() , Languages.EN.name());
    		
    		// We replace the code in the message
    		sMessage = sMessage.replace("%%CODE%%", oOTP.getSecretCode());    		
	    	
    		// Send the OTP
	    	MailUtils.sendEmail(oUser.getEmail(), sTitle, sMessage);
	    	
	    	// Return the OTP View Mode
	    	return Response.ok(oOTPViewModel).build();
    	}
		catch (Exception oEx) {
			RiseLog.errorLog("AuthResource.login: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
    }
    
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("otp")
    public Response otp(OTPViewModel oOTPVM) {
    	try {
    		ErrorViewModel oErrorViewModel = new ErrorViewModel(StringCodes.ERROR_API_WRONG_OTP.name(), Status.UNAUTHORIZED.getStatusCode());
    		
			// Validate inputs
			if (oOTPVM == null) {
				RiseLog.warnLog("AuthResource.otp: otp null, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();
			}
			if(Utils.isNullOrEmpty(oOTPVM.id)){
				RiseLog.warnLog("AuthResource.otp: otp id null or empty, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();
			}
			if(Utils.isNullOrEmpty(oOTPVM.operation)){
				RiseLog.warnLog("AuthResource.otp: otp operation null or empty, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();
			}
			if(Utils.isNullOrEmpty(oOTPVM.userProvidedCode)){
				RiseLog.warnLog("AuthResource.otp: otp user provided code null or empty, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();
			}
    		
			OTPRepository oOTPRepository = new OTPRepository();
			
			OTP oDbOTP = oOTPRepository.getOTP(oOTPVM.id);
			if (oDbOTP == null) {
				RiseLog.warnLog("AuthResource.otp: otp not found, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();
			}
			
			if (!oDbOTP.getUserId().equals(oOTPVM.userId)) {
				RiseLog.warnLog("AuthResource.otp: otp user id does not match, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();				
			}			
			
			if (!oDbOTP.getOperation().equals(oOTPVM.operation)) {
				RiseLog.warnLog("AuthResource.otp: otp operation does not match, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();				
			}
			
			if (!oDbOTP.getSecretCode().equals(oOTPVM.userProvidedCode)) {
				RiseLog.warnLog("AuthResource.otp: otp code does not match, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();				
			}
			
			Double dNow = DateUtils.getNowAsDouble();
			double dSpan = dNow - oDbOTP.getTimestamp();
			dSpan /= 1000;
			if (dSpan>RiseConfig.Current.security.maxConfirmationAgeSeconds) {
				RiseLog.warnLog("AuthResource.otp: otp code too old, user not authenticated");
				
				oOTPRepository.delete(oOTPVM.id);
				
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();								
			}
			
			// If we are here, user, operation and code are the same!
			oDbOTP.setValidated(true);
			oOTPRepository.updateOPT(oDbOTP);
			
			RiseLog.infoLog("AuthResource.otp: operation authorized id: " + oOTPVM.id + " user: " + oOTPVM.userId + " Op: " + oOTPVM.operation);
    		
    		return Response.ok().build();
    	}
		catch (Exception oEx) {
			RiseLog.errorLog("AuthResource.otp: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}    	
    }
    
    
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("login_verify")
    public Response login_verify(OTPVerifyViewModel oOTPVerifyVM) {
    	
    	try {
    		
    		ErrorViewModel oErrorViewModel = new ErrorViewModel(StringCodes.ERROR_API_WRONG_OTP.name(), Status.UNAUTHORIZED.getStatusCode());
    		
			// Validate inputs
			if (oOTPVerifyVM == null) {
				RiseLog.warnLog("AuthResource.login_verify: OTP info null, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();
			}
			if(Utils.isNullOrEmpty(oOTPVerifyVM.id)){
				RiseLog.warnLog("AuthResource.login_verify: operation id null or empty, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();
			}
			if(Utils.isNullOrEmpty(oOTPVerifyVM.userId)){
				RiseLog.warnLog("AuthResource.login_verify: user Id null or empty, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();
			}
			
			OTPRepository oOTPRepository = new OTPRepository();
			
			OTP oDbOTP = oOTPRepository.getOTP(oOTPVerifyVM.id);
			
			if (oDbOTP == null) {
				RiseLog.warnLog("AuthResource.login_verify: otp not found, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();
			}
			
			if (!oDbOTP.getUserId().equals(oOTPVerifyVM.userId)) {
				RiseLog.warnLog("AuthResource.login_verify: otp user id does not match, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();				
			}
			
			if (!oDbOTP.isValidated()) {
				RiseLog.warnLog("AuthResource.login_verify: otp not validated, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();				
			}
			
			if (!oDbOTP.getOperation().equals(OTPOperations.LOGIN.name())) {
				RiseLog.warnLog("AuthResource.login_verify: otp action not correct, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();				
			}
			
	    	// Check if we have a user
	    	UserRepository oUserRepository =  new UserRepository();
	    	User oUser = oUserRepository.getUser(oOTPVerifyVM.userId);
	    	
	    	if (oUser == null) {
	    		RiseLog.warnLog("AuthResource.login_verify: user not found");
	    		return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();
	    	}			
			
			oOTPRepository.delete(oOTPVerifyVM.id);
	    	
	    	RiseLog.debugLog("AuthResource.login_verify");	
	    	
	    	// Check if is confirmed
	    	if (oUser.getConfirmationDate() == null) {
	    		RiseLog.warnLog("AuthResource.login_verify: user not confirmed yet");
	    		return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();	    		
	    	}
	    	
	    	oUser.setLastLoginDate(DateUtils.getNowAsDouble());
	    	if (!oUserRepository.updateUser(oUser)) {
	    		RiseLog.warnLog("AuthResource.login_verify: Error updating users' last login");
	    	}
	    	
	    	double dLastPwChange = oUser.getLastPasswordUpdateDate();
	    	double dNow = DateUtils.getNowAsDouble();
	    	
	    	double dTimePassed = dNow-dLastPwChange;
	    	dTimePassed /= 1000.0;
	    	
	    	if (dTimePassed>RiseConfig.Current.security.maxPasswordAgeSeconds) {
	    		RiseLog.warnLog("AuthResource.login_verify: password expired for user " + oOTPVerifyVM.userId);
	    		
	    		oErrorViewModel = new ErrorViewModel(StringCodes.WARNING_API_PASSWORD_EXPIRED.name(), Status.TEMPORARY_REDIRECT.getStatusCode());
	    		
	    		return Response.status(Status.TEMPORARY_REDIRECT).entity(oErrorViewModel).build();
	    	}
	    	
	    	// Create the session for the user
	    	Session oSession = new Session();
	    	
	    	oSession.setLoginDate(DateUtils.getDateAsDouble(new Date()));
	    	oSession.setLastTouch(DateUtils.getDateAsDouble(new Date()));
	    	oSession.setToken(Utils.getRandomName());
	    	oSession.setUserId(oOTPVerifyVM.userId);
	    	
	    	SessionRepository oSessionRepository = new SessionRepository();
	    	oSessionRepository.add(oSession);
	    	
	    	// Save it in the view model
	    	SessionTokenViewModel oSessionTokenViewModel = (SessionTokenViewModel) RiseViewModel.getFromEntity(SessionTokenViewModel.class.getName(), oSession);
	    	
	    	// Return the entity
	    	return Response.ok(oSessionTokenViewModel).build();
    	}
		catch (Exception oEx) {
			RiseLog.errorLog("AuthResource.login_verify: " + oEx);
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
				RiseLog.warnLog("AuthResource.register: register info null");
				return Response.status(Status.BAD_REQUEST).build();
    		}
    		
    		if (oRegisterVM.admin == null) {
				RiseLog.warnLog("AuthResource.register: register admin null");
				return Response.status(Status.BAD_REQUEST).build();
    		}
    		
    		if (oRegisterVM.organization == null) {
				RiseLog.warnLog("AuthResource.register: register organization null");
				return Response.status(Status.BAD_REQUEST).build();
    		}
    		
    		if (Utils.isNullOrEmpty(oRegisterVM.organization.name)) {
				RiseLog.warnLog("AuthResource.register: register organization name null");
				return Response.status(Status.BAD_REQUEST).build();
    		}    		
    		
    		if (Utils.isNullOrEmpty(oRegisterVM.admin.userId)) {
				RiseLog.warnLog("AuthResource.register: register admin id null");
				return Response.status(Status.BAD_REQUEST).build();
    		}
    		
    		if (Utils.isNullOrEmpty(oRegisterVM.admin.email)) {
				RiseLog.warnLog("AuthResource.register: register email null");
				return Response.status(Status.BAD_REQUEST).build();
    		}    		
    		
    		if (Utils.isNullOrEmpty(oRegisterVM.password)) {
				RiseLog.warnLog("AuthResource.register: register password null");
				return Response.status(Status.BAD_REQUEST).build();
    		}
    		
    		PasswordAuthentication oPasswordAuthentication = new PasswordAuthentication();
    		
    		if (!oPasswordAuthentication.isValidPassword(oRegisterVM.password)) {
				RiseLog.warnLog("AuthResource.register: password invalid");
				return Response.status(Status.BAD_REQUEST).build();		
    		}
    		
    		if (!oPasswordAuthentication.isValidUserId(oRegisterVM.admin.userId)) {
				RiseLog.warnLog("AuthResource.register: user Id invalid");
				return Response.status(Status.BAD_REQUEST).build();		
    		}
    		
    		// Check if we have a conflict in org or user name
    		boolean bConflict = false;
    		
    		ArrayList<String> asErrors = new ArrayList<>();
    		
    		OrganizationRepository oOrganizationRepository = new OrganizationRepository();
    		
    		// Check if we have organizations with the same name
    		List<Organization> aoSameNameOrganizations = oOrganizationRepository.getOrganizationsByName(oRegisterVM.organization.name);
    		
    		if (aoSameNameOrganizations.size()>0) {
    			RiseLog.errorLog("AuthResource.register: there are already organization with this name, impossible to proceed");
    			asErrors.add(StringCodes.ERROR_API_ORG_ALREADY_EXISTS.name());
    			bConflict = true;
    		}
    		
    		// Check if we have an existing user with same user id
    		UserRepository oUserRepository = new UserRepository();
    		
    		User oPotentialExistingUser = oUserRepository.getUser(oRegisterVM.admin.userId);
    		
    		if (oPotentialExistingUser != null) {
    			RiseLog.errorLog("AuthResource.register: there are already a user with this name, impossible to proceed");
    			asErrors.add(StringCodes.ERROR_API_USER_ALREADY_EXISTS.name());
    			bConflict = true;    			
    		}
    		
    		oPotentialExistingUser = oUserRepository.getUserByEmail(oRegisterVM.admin.email);
    		
    		if (oPotentialExistingUser != null) {
    			RiseLog.errorLog("AuthResource.register: there are already a user with this email, impossible to proceed");
    			asErrors.add(StringCodes.ERROR_API_MAIL_ALREADY_EXISTS.name());
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
    		
    		RiseLog.debugLog("AuthResource.register: added organization " + oOrganization.getName() + " with Id " + oOrganization.getId());
    		
    		// Now translate the user
    		User oAdminUser = (User) RiseViewModel.copyToEntity(User.class.getName(), oRegisterVM.admin);
    		
    		// We can now assign the org id
    		oAdminUser.setOrganizationId(oOrganization.getId());
    		// Set the role
    		oAdminUser.setRole(UserRole.ADMIN);
    		
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
    		
    		oUserRepository.add(oAdminUser);
    		
    		RiseLog.debugLog("AuthResource.register: added user " + oAdminUser.getEmail() + " with Id " + oAdminUser.getUserId());
    		
    		// Get localized title and message
    		String sTitle = LangUtils.getLocalizedString(StringCodes.NOTIFICATIONS_ADMIN_CONFIRM_MAIL_TITLE.name() , Languages.EN.name());
    		String sMessage = LangUtils.getLocalizedString(StringCodes.NOTIFICATIONS_ADMIN_CONFIRM_MAIL_MESSAGE.name() , Languages.EN.name());
    		
    		
    		// Generate the confirmation Link
    		String sLink = RiseConfig.Current.security.registerConfirmAddress;
    		
    		sLink += "?code=" + sConfirmationCode + "&usr=" + URLEncoder.encode(oAdminUser.getUserId(), java.nio.charset.StandardCharsets.UTF_8.toString());
    		
    		// We replace the link in the message
    		sMessage = sMessage.replace("%%LINK%%", sLink);
    		
    		// And we send an email to the user waiting for him to confirm!
    		MailUtils.sendEmail(RiseConfig.Current.notifications.riseAdminMail, oAdminUser.getEmail(), sTitle, sMessage, true);
    		
    		return Response.ok().build();
    	}
		catch (Exception oEx) {
			RiseLog.errorLog("AuthResource.register: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
    }
    
    @GET
    @Path("confirm_adm")
    @Produces(MediaType.APPLICATION_JSON)
    public Response confirmAdminUser(@QueryParam("code") String sConfirmationCode, @QueryParam("usr") String sUserId) {
    	try {
    		if (Utils.isNullOrEmpty(sUserId)) {
				RiseLog.warnLog("AuthResource.confirmAdminUser: user id null");
				return Response.status(Status.BAD_REQUEST).build();    			
    		}
    		
    		if (Utils.isNullOrEmpty(sConfirmationCode)) {
				RiseLog.warnLog("AuthResource.confirmAdminUser: confirmation code null");
				return Response.status(Status.BAD_REQUEST).build();    			
    		}
    		
    		UserRepository oUserRepository = new UserRepository();
    		User oUser = oUserRepository.getUser(sUserId);
    		
    		if (oUser == null) {
				RiseLog.warnLog("AuthResource.confirmAdminUser: cannot find user " + sUserId);
				return Response.status(Status.BAD_REQUEST).build();    			    			
    		}
    		
    		if (!oUser.getConfirmationCode().equals(sConfirmationCode)) {
				RiseLog.warnLog("AuthResource.confirmAdminUser: wrong confirmation code " + sConfirmationCode);
				return Response.status(Status.BAD_REQUEST).build();    			
    		}
    		
    		double dRegistrationDate = oUser.getRegistrationDate();
    		double dNow = DateUtils.getNowAsDouble();
    		
    		if ((dNow-dRegistrationDate)> ( RiseConfig.Current.security.maxConfirmationAgeSeconds * 1000 ) ) {
				RiseLog.warnLog("AuthResource.confirmAdminUser: expired confirmation code " + sConfirmationCode);
				return Response.status(Status.FORBIDDEN).build();    			
    		}
    		
    		oUser.setConfirmationDate(dNow);
    		oUserRepository.updateUser(oUser);
    		
    		return Response.ok().build();
    	}
		catch (Exception oEx) {
			RiseLog.errorLog("AuthResource.confirmAdminUser: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
    }
    
    
    @POST
    @Path("confirm_usr")
    @Produces(MediaType.APPLICATION_JSON)
    public Response confirmInvitedUser(ConfirmInviteViewModel oConfirmVM) {
    	try {
    		
    		// Check we have VM
    		if (oConfirmVM == null) {
				RiseLog.warnLog("AuthResource.confirmInvitedUser: confirm VM null");
				return Response.status(Status.BAD_REQUEST).build();    			
    		}
    		
    		// Need mail
    		if (Utils.isNullOrEmpty(oConfirmVM.mail)) {
				RiseLog.warnLog("AuthResource.confirmInvitedUser: user mail null");
				return Response.status(Status.BAD_REQUEST).build();    			
    		}
    		
    		// And code 
    		if (Utils.isNullOrEmpty(oConfirmVM.confirmationCode)) {
				RiseLog.warnLog("AuthResource.confirmInvitedUser: confirmation code null");
				return Response.status(Status.BAD_REQUEST).build();    			
    		}
    		
    		// And user Id
    		if (Utils.isNullOrEmpty(oConfirmVM.userId)) {
				RiseLog.warnLog("AuthResource.confirmInvitedUser: user id null");
				return Response.status(Status.BAD_REQUEST).build();    			
    		}
    		
    		// The user should already be here
    		UserRepository oUserRepository = new UserRepository();
    		User oUser = oUserRepository.getUserByEmail(oConfirmVM.mail);
    		
    		if (oUser == null) {
				RiseLog.warnLog("AuthResource.confirmInvitedUser: cannot find user " + oConfirmVM.mail);
				return Response.status(Status.BAD_REQUEST).build();    			    			
    		}
    		
    		PasswordAuthentication oPasswordAuthentication = new PasswordAuthentication();
    		
    		if (!oPasswordAuthentication.isValidPassword(oConfirmVM.password)) {
				RiseLog.warnLog("AuthResource.confirmInvitedUser: password invalid");
				return Response.status(Status.BAD_REQUEST).build();		
    		}
    		
    		if (!oPasswordAuthentication.isValidUserId(oConfirmVM.userId)) {
				RiseLog.warnLog("AuthResource.confirmInvitedUser: user Id invalid");
				return Response.status(Status.BAD_REQUEST).build();		
    		}    		
    		
    		User oPotentialExistingUser = oUserRepository.getUser(oConfirmVM.userId);
    		
    		if (oPotentialExistingUser != null) {
    			RiseLog.errorLog("AuthResource.register: there are already a user with this email, impossible to proceed");
    			ArrayList<String> asErrors = new ArrayList<>();
    			asErrors.add(StringCodes.ERROR_API_MAIL_ALREADY_EXISTS.name());
    			ErrorViewModel oErrorViewModel = new ErrorViewModel(asErrors, Status.CONFLICT.getStatusCode());
    			return Response.status(Status.CONFLICT).entity(oErrorViewModel).build();    			
    		}    		
    		    		
    		if (oConfirmVM.acceptedPrivacy == false || oConfirmVM.acceptedTermsAndConditions == false) {
    			ErrorViewModel oErrorViewModel = new ErrorViewModel(StringCodes.ERROR_API_MISSING_ACCEPT_TERMS_AND_PRIVACY.name(), Status.CONFLICT.getStatusCode());
    			return Response.status(Status.FORBIDDEN).entity(oErrorViewModel).build();    			
    		}    		
    		
    		// The confirmation code should be the same
    		if (!oUser.getConfirmationCode().equals(oConfirmVM.confirmationCode)) {
				RiseLog.warnLog("AuthResource.confirmInvitedUser: wrong confirmation code " + oConfirmVM.confirmationCode);
				return Response.status(Status.BAD_REQUEST).build();    			
    		}
    		
    		double dRegistrationDate = oUser.getRegistrationDate();
    		double dNow = DateUtils.getNowAsDouble();
    		
    		if ((dNow-dRegistrationDate)> ( RiseConfig.Current.security.maxConfirmationAgeSeconds * 1000 ) ) {
				RiseLog.warnLog("AuthResource.confirmInvitedUser: expired confirmation code ");
				ErrorViewModel oErrorViewModel = new ErrorViewModel(StringCodes.ERROR_API_CONFIRM_EXPIRED.name(), Status.CONFLICT.getStatusCode());
				return Response.status(Status.FORBIDDEN).entity(oErrorViewModel).build();    			
    		}
    		
    		// Copy data from the view model to the new User Entity
    		User oUserToUpdate = (User) RiseViewModel.copyToEntity(User.class.getName(), oConfirmVM);
    		
    		// Add the data that was missing in the View Model:
    		
    		// Different timestamps
    		oUserToUpdate.setConfirmationDate(dNow);
    		oUserToUpdate.setLastLoginDate(dNow);
    		oUserToUpdate.setRegistrationDate(oUser.getRegistrationDate());
    		
    		// Role: is taken from the db so they cannot cheat us
    		oUserToUpdate.setRole(oUser.getRole());
    		// Organization: is taken from the db so they cannot cheat us
    		oUserToUpdate.setOrganizationId(oUser.getOrganizationId());
    		// This is the first time we set this password, must be encrypted
    		oUserToUpdate.setPassword(oPasswordAuthentication.hash(oConfirmVM.password.toCharArray()));
    		
    		// Update using e-mail because we do not have the user id yet, we are adding it now 
    		oUserRepository.updateUserByEmail(oUserToUpdate);
    		
    		// Get localized title and message
    		String sTitle = LangUtils.getLocalizedString(StringCodes.NOTIFICATIONS_USR_CONFIRMED_MAIL_TOADMIN_TITLE.name() , Languages.EN.name());
    		String sMessage = LangUtils.getLocalizedString(StringCodes.NOTIFICATIONS_USR_CONFIRMED_MAIL_TOADMIN_MESSAGE.name() , Languages.EN.name());
    		
    		// We set the new user registerd mail
    		sMessage = sMessage.replace("%%USER%%", oConfirmVM.mail);
    		
    		// Get the mails ofthe organizers
    		List<User> aoAdmins = oUserRepository.getAdminsOfOrganization(oUser.getOrganizationId());
    		String sAdmins = "";
    		for (User oAdmin : aoAdmins) {
    			sAdmins += oAdmin.getEmail() + ";";
			}
    		
    		// Send!
    		MailUtils.sendEmail(sAdmins, sTitle, sMessage);

    		// Write also to WASDI Admins
    		sTitle = "New RISE User";
    		sMessage = "Added user to RISE " + oConfirmVM.mail;
    		
    		// Just send!
    		MailUtils.sendEmail(RiseConfig.Current.notifications.riseAdminMail, sTitle, sMessage);

    		return Response.ok().build();
    	}
		catch (Exception oEx) {
			RiseLog.errorLog("AuthResource.confirmInvitedUser: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
    }

}
