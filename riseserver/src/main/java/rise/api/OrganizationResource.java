package rise.api;

import java.util.ArrayList;

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
import rise.lib.utils.Utils;
import rise.lib.utils.date.DateUtils;
import rise.lib.utils.i8n.LangUtils;
import rise.lib.utils.i8n.Languages;
import rise.lib.utils.i8n.StringCodes;
import rise.lib.utils.log.RiseLog;
import rise.lib.utils.mail.MailUtils;
import rise.lib.viewmodels.ErrorViewModel;
import rise.lib.viewmodels.InviteViewModel;
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
    		
    		if (oInviteVM == null) {
				RiseLog.warnLog("OrganizationResource.invite: invite info null");
				return Response.status(Status.BAD_REQUEST).build();
    		}    		
    		
    		if (Utils.isNullOrEmpty(oInviteVM.organizationId)) {
				RiseLog.warnLog("OrganizationResource.invite: no org received");
				return Response.status(Status.UNAUTHORIZED).build();    			
    		}

    		if (!oUser.getOrganizationId().equals(oInviteVM.organizationId)) {
				RiseLog.warnLog("OrganizationResource.invite: not your org");
				return Response.status(Status.UNAUTHORIZED).build();      			
    		}    		
    		
    		if (Utils.isNullOrEmpty(oInviteVM.email)) {
				RiseLog.warnLog("OrganizationResource.invite: invite user null");
				return Response.status(Status.BAD_REQUEST).build();
    		}
    		
    		
    		OrganizationRepository oOrganizationRepository = new OrganizationRepository();
    		
    		// Check if we have the organisation
    		Organization oOrganization = oOrganizationRepository.getOrganization(oInviteVM.organizationId);
    		
    		if (oOrganization == null) {
    			RiseLog.errorLog("OrganizationResource.invite: org not found, impossible to proceed");
    			return Response.status(Status.UNAUTHORIZED).build();
    		}
    		
    		// Check if we have an existing user with same user id
    		UserRepository oUserRepository = new UserRepository();
    		
    		User oPotentialExistingUser = oUserRepository.getUserByEmail(oInviteVM.email);
    		
    		if (oPotentialExistingUser != null) {
    			RiseLog.errorLog("OrganizationResource.invite: there are already a user with this mail, impossible to proceed");
    			ArrayList<String> asErrors = new ArrayList<>();
    			asErrors.add(StringCodes.ERROR_API_MAIL_ALREADY_EXISTS.name());
    			ErrorViewModel oErrorViewModel = new ErrorViewModel(asErrors, Status.CONFLICT.getStatusCode());
    			return Response.status(Status.CONFLICT).entity(oErrorViewModel).build();
    		}
    		
    		// Now translate the user
    		User oInvitedUser = (User) RiseViewModel.copyToEntity(User.class.getName(), oInviteVM);
    		
    		double dNow = DateUtils.getNowAsDouble();
    		
    		// Initialize the dates as now
    		oInvitedUser.setRegistrationDate(dNow);
    		
    		// Generate the Confirmation Code
    		String sConfirmationCode = Utils.getRandomName();
    		
    		// Save it
    		oInvitedUser.setConfirmationDate(null);
    		oInvitedUser.setConfirmationCode(sConfirmationCode);
    		
    		// Save the invited user
    		oUserRepository.updateUser(oInvitedUser);
    		
    		// Get localized title and message
    		String sTitle = LangUtils.getLocalizedString(StringCodes.NOTIFICATIONS_INVITE_MAIL_TITLE.name() , Languages.EN.name());
    		String sMessage = LangUtils.getLocalizedString(StringCodes.NOTIFICATIONS_INVITE_MAIL_MESSAGE.name() , Languages.EN.name());
    		
    		
    		// Generate the confirmation Link
    		String sLink = RiseConfig.Current.security.inviteConfirmAddress;
    		
    		sLink += "?code=" + sConfirmationCode + "&mail=" + oInvitedUser.getEmail();
    		
    		// We replace the link and org name in the message
    		sMessage = sMessage.replace("%%LINK%%", sLink);
    		sMessage = sMessage.replace("%%ORG%%", oOrganization.getName());
    		
    		// And we send an email to the user waiting for him to confirm!
    		MailUtils.sendEmail(RiseConfig.Current.notifications.riseAdminMail, oInvitedUser.getEmail(), sTitle, sMessage, true);
    		
    		return Response.ok().build();
    	}
		catch (Exception oEx) {
			RiseLog.errorLog("OrganizationResource.invite: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
    }
    
    
    	
}
