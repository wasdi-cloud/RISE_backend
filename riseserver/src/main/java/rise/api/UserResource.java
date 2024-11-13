
package rise.api;

import java.util.Date;

import jakarta.ws.rs.DELETE;
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
import rise.lib.business.OTP;
import rise.lib.business.OTPOperations;
import rise.lib.business.PasswordChangeRequest;

import rise.lib.business.User;
import rise.lib.config.RiseConfig;
import rise.lib.data.ChangeEmailRequestRepository;
import rise.lib.data.OTPRepository;
import rise.lib.data.PasswordChangeRequestRepository;

import rise.lib.data.UserRepository;
import rise.lib.utils.PasswordAuthentication;
import rise.lib.utils.Utils;
import rise.lib.utils.date.DateUtils;
import rise.lib.utils.i8n.LangUtils;
import rise.lib.utils.i8n.Languages;
import rise.lib.utils.i8n.StringCodes;
import rise.lib.utils.log.RiseLog;
import rise.lib.utils.mail.MailUtils;
import rise.lib.viewmodels.ChangeEmailViewModel;
import rise.lib.viewmodels.ChangePasswordRequestViewModel;
import rise.lib.viewmodels.ConfirmEmailChangeViewModel;

import rise.lib.viewmodels.ErrorViewModel;
import rise.lib.viewmodels.OTPVerifyViewModel;
import rise.lib.viewmodels.OTPViewModel;
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
			// update name , surname and mobile in user entity
			if (!Utils.isNullOrEmpty(oUserViewModel.name)) {
				oUser.setName(oUserViewModel.name);
			}
			if (!Utils.isNullOrEmpty(oUserViewModel.surname)) {
				oUser.setSurname(oUserViewModel.surname);
			}
			if (!Utils.isNullOrEmpty(oUserViewModel.mobile)) {
				oUser.setMobile(oUserViewModel.mobile);
			}
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
	public Response updateUserNotificationSettings(@HeaderParam("x-session-token") String sSessionId,
			UserViewModel oUserViewModel) {
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
	public Response changeUserEmail(@HeaderParam("x-session-token") String sSessionId,
			ChangeEmailViewModel oChangeEmailViewModel) {
		try {

			User oUser = Rise.getUserFromSession(sSessionId);
			if (oUser == null) {
				RiseLog.warnLog("UserResource.updateUser: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			if (Utils.isNullOrEmpty(oChangeEmailViewModel.newEmail)) {
				RiseLog.warnLog("UserResource.changeUserEmail: new email is null");
				return Response.status(Status.BAD_REQUEST).build();
			}
			if (Utils.isNullOrEmpty(oChangeEmailViewModel.oldEmail)) {
				RiseLog.warnLog("UserResource.changeUserEmail: old email is null");
				return Response.status(Status.BAD_REQUEST).build();
			}
			if (!oChangeEmailViewModel.oldEmail.equals(oUser.getEmail())) {
				RiseLog.warnLog("UserResource.changeUserEmail: email does not match");
				return Response.status(Status.BAD_REQUEST).build();
			}
			// check if the new email is used in another account
			UserRepository oUserRepository = new UserRepository();
			if (oUserRepository.getUserByEmail(oChangeEmailViewModel.newEmail) != null) {
				RiseLog.warnLog("UserResource.changeUserEmail: there is already an account linked to this email");
				return Response.status(Status.CONFLICT).build();
			}
			// Generate the Confirmation Code
			String sConfirmationCode = Utils.getRandomName();
			// save user confirmation code in Change email request
			ChangeEmailRequest oChangeEmailRequest = new ChangeEmailRequest();
			oChangeEmailRequest.setConfirmationCode(sConfirmationCode);
			oChangeEmailRequest.setNewEmail(oChangeEmailViewModel.newEmail);
			oChangeEmailRequest.setOldEmail(oChangeEmailViewModel.oldEmail);

			ChangeEmailRequestRepository oChnageChangeEmailRequestRepository = new ChangeEmailRequestRepository();
			oChnageChangeEmailRequestRepository.add(oChangeEmailRequest);

			// Get localized title and message
			String sTitle = LangUtils.getLocalizedString(StringCodes.NOTIFICATIONS_CHANGE_MAIL_TITLE.name(),
					Languages.EN.name());
			String sMessage = LangUtils.getLocalizedString(StringCodes.NOTIFICATIONS_CHANGE_MAIL_MESSAGE.name(),
					Languages.EN.name());

			// Generate the confirmation Link: NOTE THIS MUST TARGET The CLIENT!!
			String sLink = RiseConfig.Current.security.inviteConfirmAddress;

			sLink += "?code=" + sConfirmationCode + "?mail=" + oChangeEmailViewModel.newEmail;

			// We replace the link in the message
			sMessage = sMessage.replace("%%LINK%%", sLink);

			// And we send an email to the user waiting for him to confirm!
			MailUtils.sendEmail(RiseConfig.Current.notifications.riseAdminMail, oChangeEmailViewModel.newEmail, sTitle,
					sMessage, true);
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
			ChangeEmailRequestRepository oChangeEmailRequestRepository = new ChangeEmailRequestRepository();
			ChangeEmailRequest oChangeEmailRequest = oChangeEmailRequestRepository
					.getChangeEmailRequestByOldEmail(oConfirmVM.oldEmail);
			if (!oChangeEmailRequest.getConfirmationCode().equals(oConfirmVM.confirmationCode)) {
				RiseLog.warnLog(
						"AuthResource.confirmInvitedUser: wrong confirmation code " + oConfirmVM.confirmationCode);
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

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("change_password")
	public Response changeUserPassword(@HeaderParam("x-session-token") String sSessionId,
			ChangePasswordRequestViewModel oRequestVM) {
		try {

			User oUser = Rise.getUserFromSession(sSessionId);
			if (oUser == null) {
				RiseLog.warnLog("UserResource.updateUser: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			if (oRequestVM == null) {
				RiseLog.warnLog("UserResource.changeUserPassword: request VM null");
				return Response.status(Status.BAD_REQUEST).build();
			}
			if (Utils.isNullOrEmpty(oRequestVM.newPassword)) {
				RiseLog.warnLog("UserResource.changeUserPassword: user new password is null");
				return Response.status(Status.BAD_REQUEST).build();
			}

			if (Utils.isNullOrEmpty(oRequestVM.oldPassword)) {
				RiseLog.warnLog("UserResource.changeUserPassword: user old password is null");
				return Response.status(Status.BAD_REQUEST).build();
			}
			// check the password
			PasswordAuthentication oPasswordAuthentication = new PasswordAuthentication();
			if (!oPasswordAuthentication.authenticate(oRequestVM.oldPassword.toCharArray(), oUser.getPassword())) {
				RiseLog.warnLog("UserResource.changeUserPassword: old password not valid");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			// check the new password
			if (!oPasswordAuthentication.isValidPassword(oRequestVM.newPassword)) {
				RiseLog.warnLog("UserResource.changeUserPassword: new password invalid");
				return Response.status(Status.BAD_REQUEST).build();
			}

			// Create the OTP Entity
			OTP oOTP = new OTP();
			oOTP.setId(Utils.getRandomName());
			oOTP.setSecretCode(Utils.getOTPPassword());
			oOTP.setUserId(oUser.getUserId());
			oOTP.setValidated(false);
			oOTP.setOperation(OTPOperations.CHANGE_PASSWORD.name());
			oOTP.setTimestamp(DateUtils.getNowAsDouble());

			// Add it to the Db
			OTPRepository oOTPRepository = new OTPRepository();
			oOTPRepository.add(oOTP);

			RiseLog.debugLog("UserResource.changeUserPassword: created OTP " + oOTP.getId());

			PasswordChangeRequest oPasswordChangeRequest = new PasswordChangeRequest();
			PasswordChangeRequestRepository oPasswordChangeRequestRepository = new PasswordChangeRequestRepository();
			// saving new password with the otp id to later retrieve it
			oPasswordChangeRequest.setOtpId(oOTP.getId());
			oPasswordChangeRequest.setPassword(oRequestVM.newPassword);
			oPasswordChangeRequest.setUserId(oOTP.getUserId());
			oPasswordChangeRequestRepository.add(oPasswordChangeRequest);

			// Create the view model
			OTPViewModel oOTPViewModel = new OTPViewModel();
			oOTPViewModel = (OTPViewModel) RiseViewModel.getFromEntity(OTPViewModel.class.getName(), oOTP);

			// Create the verify API address
			oOTPViewModel.verifyAPI = RiseConfig.Current.serverApiAddress;
			if (!oOTPViewModel.verifyAPI.endsWith("/"))
				oOTPViewModel.verifyAPI += "/";
			oOTPViewModel.verifyAPI += "usr/verify_password_change";

			// Get localized title and message
			String sTitle = LangUtils.getLocalizedString(StringCodes.OTP_TITLE.name(), Languages.EN.name());
			String sMessage = LangUtils.getLocalizedString(StringCodes.OTP_MESSAGE.name(), Languages.EN.name());

			// We replace the code in the message
			sMessage = sMessage.replace("%%CODE%%", oOTP.getSecretCode());

			// Send the OTP
			MailUtils.sendEmail(oUser.getEmail(), sTitle, sMessage);

			// Return the OTP View Mode
			return Response.ok(oOTPViewModel).build();
		} catch (Exception oEx) {
			RiseLog.errorLog("UserResource.changeUserPassword: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("change_password_verify")
	public Response verifyPasswordChange(OTPVerifyViewModel oOTPVerifyVM) {

		try {

			ErrorViewModel oErrorViewModel = new ErrorViewModel(StringCodes.ERROR_API_WRONG_OTP.name(),
					Status.UNAUTHORIZED.getStatusCode());
			// Validate inputs
			if (oOTPVerifyVM == null) {
				RiseLog.warnLog("UserResource.verifyPasswordChange: OTP info null, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();
			}
			if (Utils.isNullOrEmpty(oOTPVerifyVM.id)) {
				RiseLog.warnLog(
						"UserResource.verifyPasswordChange: operation id null or empty, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();
			}
			if (Utils.isNullOrEmpty(oOTPVerifyVM.userId)) {
				RiseLog.warnLog("UserResource.verifyPasswordChange: user Id null or empty, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();
			}

			OTPRepository oOTPRepository = new OTPRepository();

			OTP oDbOTP = oOTPRepository.getOTP(oOTPVerifyVM.id);

			if (oDbOTP == null) {
				RiseLog.warnLog("UserResource.verifyPasswordChange: otp not found, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();
			}

			if (!oDbOTP.getUserId().equals(oOTPVerifyVM.userId)) {
				RiseLog.warnLog(
						"UserResource.verifyPasswordChange: otp user id does not match, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();
			}

			if (!oDbOTP.isValidated()) {
				RiseLog.warnLog("UserResource.verifyPasswordChange: otp not validated, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();
			}

			if (!oDbOTP.getOperation().equals(OTPOperations.CHANGE_PASSWORD.name())) {
				RiseLog.warnLog("UserResource.verifyPasswordChange: otp action not correct, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();
			}

			// Check if we have a user
			UserRepository oUserRepository = new UserRepository();
			User oUser = oUserRepository.getUser(oOTPVerifyVM.userId);

			if (oUser == null) {
				RiseLog.warnLog("UserResource.verifyPasswordChange: user not found");
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();
			}
			PasswordAuthentication oPasswordAuthentication = new PasswordAuthentication();
			PasswordChangeRequestRepository oPasswordChangeRequestRepository = new PasswordChangeRequestRepository();
			PasswordChangeRequest oChangeRequest = oPasswordChangeRequestRepository
					.getPasswordChangeRequestByOTPId(oOTPVerifyVM.id);
			oUser.setPassword(oPasswordAuthentication.hash(oChangeRequest.getPassword().toCharArray()));
			oUser.setLastPasswordUpdateDate(DateUtils.getNowAsDouble());
			oOTPRepository.delete(oOTPVerifyVM.id);
			oUserRepository.updateUser(oUser);
			RiseLog.debugLog("UserResource.verifyPasswordChange");
			return Response.ok().build();

		} catch (Exception oEx) {
			RiseLog.errorLog("UserResource.verifyPasswordChange: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("delete-user")
	public Response deleteUser(@HeaderParam("x-session-token") String sSessionId) {
		try {
			User oUser = Rise.getUserFromSession(sSessionId);

			if (oUser == null) {
				RiseLog.warnLog("UserResource.getUser: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			// Create the OTP Entity
			OTP oOTP = new OTP();
			oOTP.setId(Utils.getRandomName());
			oOTP.setSecretCode(Utils.getOTPPassword());
			oOTP.setUserId(oUser.getUserId());
			oOTP.setValidated(false);
			oOTP.setOperation(OTPOperations.DELETE_USER.name());
			oOTP.setTimestamp(DateUtils.getNowAsDouble());

			// Add it to the Db
			OTPRepository oOTPRepository = new OTPRepository();
			oOTPRepository.add(oOTP);

			RiseLog.debugLog("UserResource.deleteUser: created OTP " + oOTP.getId());

			// Create the view model
			OTPViewModel oOTPViewModel = new OTPViewModel();
			oOTPViewModel = (OTPViewModel) RiseViewModel.getFromEntity(OTPViewModel.class.getName(), oOTP);

			// Create the verify API address
			oOTPViewModel.verifyAPI = RiseConfig.Current.serverApiAddress;
			if (!oOTPViewModel.verifyAPI.endsWith("/"))
				oOTPViewModel.verifyAPI += "/";
			oOTPViewModel.verifyAPI += "usr/verify_delete_user";

			// Get localized title and message
			String sTitle = LangUtils.getLocalizedString(StringCodes.OTP_TITLE.name(), Languages.EN.name());
			String sMessage = LangUtils.getLocalizedString(StringCodes.OTP_MESSAGE.name(), Languages.EN.name());

			// We replace the code in the message
			sMessage = sMessage.replace("%%CODE%%", oOTP.getSecretCode());

			// Send the OTP
			MailUtils.sendEmail(oUser.getEmail(), sTitle, sMessage);

			// Return the OTP View Mode
			return Response.ok(oOTPViewModel).build();
		} catch (Exception oEx) {
			RiseLog.errorLog("UserResource.deleteUser: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@DELETE
	@Path("verify_delete_user")
	public Response verifyDeleteUser(OTPVerifyViewModel oOTPVerifyVM) {
		try {

			ErrorViewModel oErrorViewModel = new ErrorViewModel(StringCodes.ERROR_API_WRONG_OTP.name(),
					Status.UNAUTHORIZED.getStatusCode());

			// Validate inputs
			if (oOTPVerifyVM == null) {
				RiseLog.warnLog("UserResource.verifyDeleteUser: OTP info null, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();
			}
			if (Utils.isNullOrEmpty(oOTPVerifyVM.id)) {
				RiseLog.warnLog("UserResource.verifyDeleteUser: operation id null or empty, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();
			}
			if (Utils.isNullOrEmpty(oOTPVerifyVM.userId)) {
				RiseLog.warnLog("UserResource.verifyDeleteUser: user Id null or empty, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();
			}

			OTPRepository oOTPRepository = new OTPRepository();

			OTP oDbOTP = oOTPRepository.getOTP(oOTPVerifyVM.id);

			if (oDbOTP == null) {
				RiseLog.warnLog("UserResource.verifyDeleteUser: otp not found, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();
			}

			if (!oDbOTP.getUserId().equals(oOTPVerifyVM.userId)) {
				RiseLog.warnLog("UserResource.verifyDeleteUser: otp user id does not match, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();
			}

			if (!oDbOTP.isValidated()) {
				RiseLog.warnLog("UserResource.verifyDeleteUser: otp not validated, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();
			}

			if (!oDbOTP.getOperation().equals(OTPOperations.DELETE_USER.name())) {
				RiseLog.warnLog("UserResource.verifyDeleteUser: otp action not correct, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();
			}

			// Check if we have a user
			UserRepository oUserRepository = new UserRepository();
			User oUser = oUserRepository.getUser(oOTPVerifyVM.userId);

			if (oUser == null) {
				RiseLog.warnLog("UserResource.verifyDeleteUser: user not found");
				return Response.status(Status.UNAUTHORIZED).entity(oErrorViewModel).build();
			}
			oOTPRepository.delete(oOTPVerifyVM.id);
			// delete user
			oUserRepository.deleteByUserId(oUser.getUserId());
			return Response.ok().build();
		} catch (Exception oEx) {
			RiseLog.errorLog("UserResource.verifyDeleteUser: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

}