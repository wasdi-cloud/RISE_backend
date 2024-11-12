
package rise.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import rise.Rise;
import rise.lib.business.ChangeEmailRequest;
import rise.lib.business.User;
import rise.lib.config.RiseConfig;
import rise.lib.data.ChangeEmailRequestRepository;
import rise.lib.data.UserRepository;
import rise.lib.utils.Utils;
import rise.lib.utils.i8n.LangUtils;
import rise.lib.utils.i8n.Languages;
import rise.lib.utils.i8n.StringCodes;
import rise.lib.utils.log.RiseLog;
import rise.lib.utils.mail.MailUtils;
import rise.lib.viewmodels.ChangeEmailViewModel;
import rise.lib.viewmodels.ConfirmEmailChangeViewModel;
import rise.lib.viewmodels.ConfirmInviteViewModel;
import rise.lib.viewmodels.RiseViewModel;
import rise.lib.viewmodels.UserViewModel;

@Path("usr")
public class UserResource {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getUser(@HeaderParam("x-session-token") String sSessionId) {
		try {
			User oUser = Rise.getUserFromSession(sSessionId);

			if (oUser == null) {
				RiseLog.warnLog("UserResource.getUser: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			UserViewModel oUserViewModel = (UserViewModel) RiseViewModel.getFromEntity(UserViewModel.class.getName(),
					oUser);

			return Response.ok(oUserViewModel).build();
		} catch (Exception oEx) {
			RiseLog.errorLog("UserResource.getUser: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@POST
	public Response updateUser(@HeaderParam("x-session-token") String sSessionId, UserViewModel oUserViewModel) {
		try {

			User oUser = Rise.getUserFromSession(sSessionId);
			if (oUser == null) {
				RiseLog.warnLog("UserResource.updateUser: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			if (oUserViewModel == null) {
				RiseLog.warnLog("UserResource.updateUser: user VM null");
				return Response.status(Status.BAD_REQUEST).build();
			}
			if (oUserViewModel.name == null) {
				RiseLog.warnLog("UserResource.updateUser: user name null");
				return Response.status(Status.BAD_REQUEST).build();
			}
			if (oUserViewModel.surname == null) {
				RiseLog.warnLog("UserResource.updateUser: user surname null");
				return Response.status(Status.BAD_REQUEST).build();
			}

			if (oUserViewModel.mobile == null) {
				RiseLog.warnLog("UserResource.updateUser: user mobile null");
				return Response.status(Status.BAD_REQUEST).build();
			}

			// update name , surname and mobile in user entity
			oUser.setName(oUserViewModel.name);
			oUser.setSurname(oUserViewModel.surname);
			oUser.setMobile(oUserViewModel.mobile);

			UserRepository oUserRepository = new UserRepository();
			oUserRepository.updateUser(oUser);
			
			
			return Response.ok().build();
		} catch (Exception oEx) {
			RiseLog.errorLog("UserResource.updateUser: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	@POST
	@Path("notification-update")
	public Response updateUserNotificationSettings(@HeaderParam("x-session-token") String sSessionId, UserViewModel oUserViewModel) {
		try {

			User oUser = Rise.getUserFromSession(sSessionId);
			if (oUser == null) {
				RiseLog.warnLog("UserResource.updateUserNotificationSettings: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			if (oUserViewModel == null) {
				RiseLog.warnLog("UserResource.updateUserNotificationSettings: user VM null");
				return Response.status(Status.BAD_REQUEST).build();
			}
			

			// update name , surname and mobile in user entity
			oUser.setNotifyActivities(oUserViewModel.notifyActivities);
			oUser.setNotifyMaintenance(oUserViewModel.notifyMaintenance);
			oUser.setNotifyNewsletter(oUserViewModel.notifyNewsletter);

			UserRepository oUserRepository = new UserRepository();
			oUserRepository.updateUser(oUser);
			
			
			return Response.ok().build();
		} catch (Exception oEx) {
			RiseLog.errorLog("UserResource.updateUserNotificationSettings: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@POST
	@Path("change-email")
	public Response changeUserEmail(@HeaderParam("x-session-token") String sSessionId, ChangeEmailViewModel oChangeEmailViewModel) {
		try {

			User oUser = Rise.getUserFromSession(sSessionId);
			if (oUser == null) {
				RiseLog.warnLog("UserResource.updateUser: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			if(oChangeEmailViewModel.newEmail==null) {
				RiseLog.warnLog("UserResource.changeUserEmail: new email is null");
				return Response.status(Status.BAD_REQUEST).build();
			}
			if(oChangeEmailViewModel.oldEmail==null) {
				RiseLog.warnLog("UserResource.changeUserEmail: old email is null");
				return Response.status(Status.BAD_REQUEST).build();
			}
			if(!oChangeEmailViewModel.oldEmail.equals(oUser.getEmail())) {
				RiseLog.warnLog("UserResource.changeUserEmail: email does not match");
				return Response.status(Status.BAD_REQUEST).build();
			}
			//check if the new email is used in another account
			UserRepository oUserRepository=new UserRepository();
			if(oUserRepository.getUserByEmail(oChangeEmailViewModel.newEmail)!=null) {
				RiseLog.warnLog("UserResource.changeUserEmail: there is already an account linked to this email");
				return Response.status(Status.CONFLICT).build();
			}
			// Generate the Confirmation Code
    		String sConfirmationCode = Utils.getRandomName();
    		//save user confirmation code in Change email request
    		ChangeEmailRequest oChangeEmailRequest=new ChangeEmailRequest();
    		oChangeEmailRequest.setConfirmationCode(sConfirmationCode);
    		oChangeEmailRequest.setNewEmail(oChangeEmailViewModel.newEmail);
    		oChangeEmailRequest.setOldEmail(oChangeEmailViewModel.oldEmail);
    		
    		ChangeEmailRequestRepository oChnageChangeEmailRequestRepository = new ChangeEmailRequestRepository();
    		oChnageChangeEmailRequestRepository.add(oChangeEmailRequest);
    		
    		// Get localized title and message
    		String sTitle = LangUtils.getLocalizedString(StringCodes.NOTIFICATIONS_INVITE_MAIL_TITLE.name() , Languages.EN.name());
    		String sMessage = LangUtils.getLocalizedString(StringCodes.NOTIFICATIONS_INVITE_MAIL_MESSAGE.name() , Languages.EN.name());
    		
    		
    		// Generate the confirmation Link: NOTE THIS MUST TARGET The CLIENT!!
    		String sLink = RiseConfig.Current.security.inviteConfirmAddress;
    		
    		sLink += "?code=" + sConfirmationCode +"?mail="+oChangeEmailViewModel.newEmail;
    		
    		// We replace the link and org name in the message
    		sMessage = sMessage.replace("%%LINK%%", sLink);
    		
    		
    		// And we send an email to the user waiting for him to confirm!
    		MailUtils.sendEmail(RiseConfig.Current.notifications.riseAdminMail, oChangeEmailViewModel.newEmail, sTitle, sMessage, true);	
			return Response.ok().build();
		} catch (Exception oEx) {
			RiseLog.errorLog("UserResource.changeUserEmail: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	@POST
	@Path("confirm-new-email")
	public Response confirmUserNewEmail(ConfirmEmailChangeViewModel oConfirmVM) {
	    try {
	    	// Check we have VM
    		if (oConfirmVM == null) {
				RiseLog.warnLog("AuthResource.confirmInvitedUser: confirm VM null");
				return Response.status(Status.BAD_REQUEST).build();    			
    		}
    		
    		// Need new mail
    		if (Utils.isNullOrEmpty(oConfirmVM.newEmail)) {
				RiseLog.warnLog("AuthResource.confirmInvitedUser: user mail null");
				return Response.status(Status.BAD_REQUEST).build();    			
    		}
    		
    		// And code 
    		if (Utils.isNullOrEmpty(oConfirmVM.confirmationCode)) {
				RiseLog.warnLog("AuthResource.confirmInvitedUser: confirmation code null");
				return Response.status(Status.BAD_REQUEST).build();    			
    		}
    		// Need old mail
    		if (Utils.isNullOrEmpty(oConfirmVM.oldEmail)) {
				RiseLog.warnLog("AuthResource.confirmInvitedUser: user mail null");
				return Response.status(Status.BAD_REQUEST).build();    			
    		}
    		
    		// The user should already be here
    		UserRepository oUserRepository = new UserRepository();
    		User oUser = oUserRepository.getUserByEmail(oConfirmVM.oldEmail);
    		
    		if (oUser == null) {
				RiseLog.warnLog("AuthResource.confirmInvitedUser: cannot find user " + oConfirmVM.oldEmail);
				return Response.status(Status.BAD_REQUEST).build();    			    			
    		}
    		// The confirmation code should be the same
    		ChangeEmailRequestRepository oChangeEmailRequestRepository=new ChangeEmailRequestRepository();
    		ChangeEmailRequest oChangeEmailRequest=oChangeEmailRequestRepository.getChangeEmailRequestByOldEmail(oConfirmVM.oldEmail);
    		if (!oChangeEmailRequest.getConfirmationCode().equals(oConfirmVM.confirmationCode)) {
				RiseLog.warnLog("AuthResource.confirmInvitedUser: wrong confirmation code " + oConfirmVM.confirmationCode);
				return Response.status(Status.BAD_REQUEST).build();    			
    		}
    		
    		oUser.setEmail(oConfirmVM.newEmail);
    		oUserRepository.updateUser(oUser);

	        return Response.ok("Email successfully updated").build();
	    } catch (Exception oEx) {
	        RiseLog.errorLog("UserResource.confirmNewEmail: " + oEx);
	        return Response.status(Status.INTERNAL_SERVER_ERROR).build();
	    }
	}

	
}