package rise.api;

import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import rise.Rise;
import rise.lib.business.Organization;
import rise.lib.business.User;
import rise.lib.business.UserRole;
import rise.lib.config.RiseConfig;
import rise.lib.data.OrganizationRepository;
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
import rise.lib.viewmodels.InviteViewModel;
import rise.lib.viewmodels.RegisterViewModel;
import rise.lib.viewmodels.RiseViewModel;

@Path("org")
public class OrganizationResource {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("invite")
    public Response invite(@HeaderParam("x-session-token") String sSessionId, InviteViewModel oInviteVM) {
    	
    	try {
    		
    		User oUser = Rise.getUserFromSession(sSessionId);
    		
    		if (oUser == null) {
				RiseLog.warnLog("OrganizationResource.invite: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();    			
    		}
    		
    		if (!oUser.getRole().equals(UserRole.ADMIN)) {
				RiseLog.warnLog("OrganizationResource.invite: not an admin");
				return Response.status(Status.UNAUTHORIZED).build();      			
    		}

    		if (!oUser.getOrganizationId().equals(oInviteVM.organizationId)) {
				RiseLog.warnLog("OrganizationResource.invite: not your org");
				return Response.status(Status.UNAUTHORIZED).build();      			
    		}
    		
    		if (oInviteVM == null) {
				RiseLog.warnLog("OrganizationResource.invite: invite info null");
				return Response.status(Status.BAD_REQUEST).build();
    		}
    		
    		if (Utils.isNullOrEmpty(oInviteVM.email)) {
				RiseLog.warnLog("OrganizationResource.invite: invite user null");
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
    		
    		if (oPotentialExistingUser == null) {
    			RiseLog.errorLog("AuthResource.register: there are already a user with this name, impossible to proceed");
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
			RiseLog.errorLog("AuthResource.register: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
    }
    	
}
