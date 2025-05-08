package rise.api;

import java.util.ArrayList;

import java.util.List;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import rise.Rise;
import rise.lib.business.Area;
import rise.lib.business.*;

import rise.lib.config.RiseConfig;
import rise.lib.data.AreaRepository;
import rise.lib.data.AreaRepository;
import rise.lib.data.OTPRepository;
import rise.lib.data.OrganizationRepository;

import rise.lib.data.UserRepository;
import rise.lib.utils.PermissionsUtils;
import rise.lib.utils.Utils;
import rise.lib.utils.date.DateUtils;
import rise.lib.utils.i8n.LangUtils;
import rise.lib.utils.i8n.Languages;
import rise.lib.utils.i8n.StringCodes;
import rise.lib.utils.log.RiseLog;
import rise.lib.utils.mail.MailUtils;
import rise.lib.viewmodels.ErrorViewModel;
import rise.lib.viewmodels.InviteViewModel;
import rise.lib.viewmodels.OTPVerifyViewModel;
import rise.lib.viewmodels.OTPViewModel;
import rise.lib.viewmodels.OrganizationViewModel;
import rise.lib.viewmodels.RiseViewModel;
import rise.lib.viewmodels.UserViewModel;

@Path("org")
public class OrganizationResource {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("by_usr")
	public Response getByUser(@HeaderParam("x-session-token") String sSessionId, InviteViewModel oInviteVM) {
		try {
			User oUser = Rise.getUserFromSession(sSessionId);

			if (oUser == null) {
				RiseLog.warnLog("OrganizationResource.getByUser: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			if (Utils.isNullOrEmpty(oUser.getOrganizationId())) {
				RiseLog.warnLog("OrganizationResource.getByUser: not valid org for the user");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			OrganizationRepository oOrganizationRepository = new OrganizationRepository();
			Organization oOrganization = oOrganizationRepository.getOrganization(oUser.getOrganizationId());

			if (oOrganization == null) {
				RiseLog.warnLog("OrganizationResource.getByUser: the org " + oUser.getOrganizationId() + " of user "
						+ oUser.getUserId() + " does not exists in the db");
				return Response.status(Status.BAD_REQUEST).build();
			}

			OrganizationViewModel oOrganizationViewModel = (OrganizationViewModel) RiseViewModel
					.getFromEntity(OrganizationViewModel.class.getName(), oOrganization);

			return Response.ok(oOrganizationViewModel).build();
		} catch (Exception oEx) {
			RiseLog.errorLog("OrganizationResource.getByUser: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response get(@HeaderParam("x-session-token") String sSessionId, @QueryParam("id") String sId) {
		try {
			User oUser = Rise.getUserFromSession(sSessionId);

			if (oUser == null) {
				RiseLog.warnLog("OrganizationResource.get: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			OrganizationRepository oOrganizationRepository = new OrganizationRepository();
			Organization oOrganization = oOrganizationRepository.getOrganization(sId);

			if (!PermissionsUtils.canUserAccessOrganization(oOrganization, oUser)) {
				RiseLog.warnLog("OrganizationResource.get: user cannot access org");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			if (oOrganization == null) {
				RiseLog.warnLog("OrganizationResource.getByUser: the org " + oUser.getOrganizationId() + " of user "
						+ oUser.getUserId() + " does not exists in the db");
				return Response.status(Status.BAD_REQUEST).build();
			}

			OrganizationViewModel oOrganizationViewModel = (OrganizationViewModel) RiseViewModel
					.getFromEntity(OrganizationViewModel.class.getName(), oOrganization);

			return Response.ok(oOrganizationViewModel).build();
		} catch (Exception oEx) {
			RiseLog.errorLog("OrganizationResource.getByUser: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

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

			if (!(oUser.getRole().equals(UserRole.ADMIN) || oUser.getRole().equals(UserRole.RISE_ADMIN))) {
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
				RiseLog.errorLog(
						"OrganizationResource.invite: there are already a user with this mail, impossible to proceed");
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
			oUserRepository.add(oInvitedUser);

			// Get localized title and message
			String sTitle = LangUtils.getLocalizedString(StringCodes.NOTIFICATIONS_INVITE_MAIL_TITLE.name(),
					Languages.EN.name());
			String sMessage = LangUtils.getLocalizedString(StringCodes.NOTIFICATIONS_INVITE_MAIL_MESSAGE.name(),
					Languages.EN.name());

			// Generate the confirmation Link: NOTE THIS MUST TARGET The CLIENT!!
			String sLink = RiseConfig.Current.security.inviteConfirmAddress;

			sLink += "?code=" + sConfirmationCode + "&mail=" + oInvitedUser.getEmail();

			// We replace the link and org name in the message
			sMessage = sMessage.replace("%%LINK%%", sLink);
			sMessage = sMessage.replace("%%ORG%%", oOrganization.getName());

			// And we send an email to the user waiting for him to confirm!
			MailUtils.sendEmail(RiseConfig.Current.notifications.riseAdminMail, oInvitedUser.getEmail(), sTitle,
					sMessage, true);

			return Response.ok().build();
		} catch (Exception oEx) {
			RiseLog.errorLog("OrganizationResource.invite: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@PUT
	public Response updateOrganization(@HeaderParam("x-session-token") String sSessionId,
			OrganizationViewModel oOrganizationViewModel) {
		try {

			User oUser = Rise.getUserFromSession(sSessionId);

			if (oUser == null) {
				RiseLog.warnLog("OrganizationResource.updateOrganization: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			if (!(oUser.getRole().equals(UserRole.ADMIN) || oUser.getRole().equals(UserRole.RISE_ADMIN))) {
				RiseLog.warnLog("OrganizationResource.updateOrganization: not an admin");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			if (oOrganizationViewModel == null) {
				RiseLog.warnLog("OrganizationResource.updateOrganization: organization VM is null");
				return Response.status(Status.BAD_REQUEST).build();
			}
			if (!oUser.getOrganizationId().equals(oOrganizationViewModel.id)) {
				RiseLog.warnLog("OrganizationResource.updateOrganization: invalid permission");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			OrganizationRepository oOrganizationRepository = new OrganizationRepository();
			Organization oOrganization = oOrganizationRepository.getOrganization(oUser.getOrganizationId());
			if (!Utils.isNullOrEmpty(oOrganizationViewModel.city)) {
				oOrganization.setCity(oOrganizationViewModel.city);
			}
			if (!Utils.isNullOrEmpty(oOrganizationViewModel.country)) {
				oOrganization.setCountry(oOrganizationViewModel.country);
			}
			if (!Utils.isNullOrEmpty(oOrganizationViewModel.creationDate)) {
				oOrganization.setCreationDate(oOrganizationViewModel.creationDate);
			}
			if (!Utils.isNullOrEmpty(oOrganizationViewModel.name)) {
				oOrganization.setName(oOrganizationViewModel.name);
			}
			if (!Utils.isNullOrEmpty(oOrganizationViewModel.number)) {
				oOrganization.setNumber(oOrganizationViewModel.number);
			}
			if (!Utils.isNullOrEmpty(oOrganizationViewModel.internationalPrefix)) {
				oOrganization.setInternationalPrefix(oOrganizationViewModel.internationalPrefix);
			}			
			if (!Utils.isNullOrEmpty(oOrganizationViewModel.phone)) {
				oOrganization.setPhone(oOrganizationViewModel.phone);
			}
			if (!Utils.isNullOrEmpty(oOrganizationViewModel.postalCode)) {
				oOrganization.setPostalCode(oOrganizationViewModel.postalCode);
			}
			if (!Utils.isNullOrEmpty(oOrganizationViewModel.street)) {
				oOrganization.setStreet(oOrganizationViewModel.street);
			}
			if (!Utils.isNullOrEmpty(oOrganizationViewModel.type)) {
				oOrganization.setType(oOrganizationViewModel.type);
			}
			if (!Utils.isNullOrEmpty(oOrganizationViewModel.vat)) {
				oOrganization.setVat(oOrganizationViewModel.vat);
			}
			oOrganizationRepository.update(oOrganization, oOrganization.getId());
			return Response.ok().build();
		} catch (Exception oEx) {
			RiseLog.errorLog("OrganizationResource.updateOrganization: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("list_users")
	public Response getOrganizationUsers(@HeaderParam("x-session-token") String sSessionId) {
		try {
			List<UserViewModel> aoOrgUsers = new ArrayList<>();
			User oUser = Rise.getUserFromSession(sSessionId);

			if (oUser == null) {
				RiseLog.warnLog("OrganizationResource.getOrganizationUsers: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			if (!(oUser.getRole().equals(UserRole.ADMIN) || oUser.getRole().equals(UserRole.RISE_ADMIN))) {
				RiseLog.warnLog("OrganizationResource.getOrganizationUsers: not an admin");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			String sOrganizationId = oUser.getOrganizationId();
			UserRepository oUserRepository = new UserRepository();
			List<User> aoUsers = oUserRepository.getUsersByOrganizationId(sOrganizationId);
			for (User oUserFromList : aoUsers) {
				
				//check to see if it is activated ( for invited users)
				if(oUserFromList.getConfirmationDate()==null) {
					continue;
				}
				//check if it is current user
				if(oUserFromList.getUserId().equals(oUser.getUserId())) {
					continue;
				}
				UserViewModel oUserViewModel = (UserViewModel) RiseViewModel
						.getFromEntity(UserViewModel.class.getName(), oUserFromList);
				aoOrgUsers.add(oUserViewModel);
			}
			return Response.ok(aoOrgUsers).build();
		} catch (Exception oEx) {
			RiseLog.errorLog("OrganizationResource.getOrganizationUsers: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@DELETE
	@Path("remove-user")
	public Response removeUsersFromOrganzation(@HeaderParam("x-session-token") String sSessionId,
			List<UserViewModel> aoUsersToDelete) {
		try {
			User oUser = Rise.getUserFromSession(sSessionId);
			if (oUser == null) {
				RiseLog.warnLog("OrganizationResource.removeUsersFromOrganzation: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			if (!(oUser.getRole().equals(UserRole.ADMIN) || oUser.getRole().equals(UserRole.RISE_ADMIN))) {
				RiseLog.warnLog("OrganizationResource.removeUsersFromOrganzation: not an admin");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			if (aoUsersToDelete == null || aoUsersToDelete.size() == 0) {
				RiseLog.warnLog("OrganizationResource.removeUsersFromOrganzation: list is empty");
				return Response.status(Status.BAD_REQUEST).build();
			}
			for (UserViewModel oUserViewModel : aoUsersToDelete) {
				if (Utils.isNullOrEmpty(oUserViewModel.userId)) {
					RiseLog.warnLog("OrganizationResource.removeUsersFromOrganzation: user id of " + oUserViewModel
							+ "is  empty");
					return Response.status(Status.BAD_REQUEST).build();
				}
			}
			UserRepository oUserRepository = new UserRepository();
			for (UserViewModel oUserViewModel : aoUsersToDelete) {
				oUserRepository.deleteByUserId(oUserViewModel.userId);
			}
			return Response.ok().build();
		} catch (Exception oEx) {
			RiseLog.errorLog("OrganizationResource.removeUsersFromOrganzation: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	@Path("delete-org")
	public Response deleteOrganzation(@HeaderParam("x-session-token") String sSessionId) {
		try {
			User oUser = Rise.getUserFromSession(sSessionId);
			if (oUser == null) {
				RiseLog.warnLog("OrganizationResource.deleteOrganzation: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			if (!PermissionsUtils.isAdmin(oUser)) {
				RiseLog.warnLog("OrganizationResource.removeUsersFromOrganzation: not an admin");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			// create otp

			// Create the OTP Entity
			OTP oOTP = new OTP();
			oOTP.setId(Utils.getRandomName());
			oOTP.setSecretCode(Utils.getOTPPassword());
			oOTP.setUserId(oUser.getUserId());
			oOTP.setValidated(false);
			oOTP.setOperation(OTPOperations.DELETE_ORG.name());
			oOTP.setTimestamp(DateUtils.getNowAsDouble());

			// Add it to the Db
			OTPRepository oOTPRepository = new OTPRepository();
			oOTPRepository.add(oOTP);

			RiseLog.debugLog("OrganizationResource.deleteOrganization: created OTP " + oOTP.getId());
			// Create the view model
			OTPViewModel oOTPViewModel = new OTPViewModel();
			oOTPViewModel = (OTPViewModel) RiseViewModel.getFromEntity(OTPViewModel.class.getName(), oOTP);

			// Create the verify API address
			oOTPViewModel.verifyAPI = RiseConfig.Current.serverApiAddress;
			if (!oOTPViewModel.verifyAPI.endsWith("/"))
				oOTPViewModel.verifyAPI += "/";
			oOTPViewModel.verifyAPI += "org/verify-delete-org";

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
			RiseLog.errorLog("OrganizationResource.removeUsersFromOrganzation: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	@Path("verify-delete-org")
	public Response verifyDeleteOrg(@HeaderParam("x-session-token") String sSessionId, OTPVerifyViewModel oOTPVerifyVM) {
		try {

			// Validate inputs
			if (oOTPVerifyVM == null) {
				RiseLog.warnLog("OrganizationResource.verifyDeleteOrg: OTP info null, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			if (Utils.isNullOrEmpty(oOTPVerifyVM.id)) {
				RiseLog.warnLog(
						"OrganizationResource.verifyDeleteOrg: operation id null or empty, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			if (Utils.isNullOrEmpty(oOTPVerifyVM.userId)) {
				RiseLog.warnLog("OrganizationResource.verifyDeleteOrg: user Id null or empty, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			OTPRepository oOTPRepository = new OTPRepository();

			OTP oDbOTP = oOTPRepository.getOTP(oOTPVerifyVM.id);

			if (oDbOTP == null) {
				RiseLog.warnLog("OrganizationResource.verifyDeleteOrg: otp not found, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			if (!oDbOTP.getUserId().equals(oOTPVerifyVM.userId)) {
				RiseLog.warnLog("OrganizationResource.verifyDeleteOrg: otp user id does not match, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			if (!oDbOTP.isValidated()) {
				RiseLog.warnLog("OrganizationResource.verifyDeleteOrg: otp not validated, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			if (!oDbOTP.getOperation().equals(OTPOperations.DELETE_ORG.name())) {
				RiseLog.warnLog("OrganizationResource.verifyDeleteOrg: otp action not correct, user not authenticated");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			// Check if we have a user
			UserRepository oUserRepository = new UserRepository();
			User oUser = oUserRepository.getUser(oOTPVerifyVM.userId);

			if (oUser == null) {
				RiseLog.warnLog("OrganizationResource.verifyDeleteOrg: user not found");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			if (!PermissionsUtils.isAdmin(oUser)) {
				RiseLog.warnLog("OrganizationResource.verifyDeleteOrg: user not admin");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			oOTPRepository.delete(oOTPVerifyVM.id);

			this.internalDeleteOrganization(sSessionId, oUser);

			return Response.ok().build();
		} catch (Exception oEx) {
			RiseLog.errorLog("OrganizationResource.verifyDeleteOrg: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	public void internalDeleteOrganization(String sSessionId, User oUser) {
		UserRepository oUserRepository = new UserRepository();

		OrganizationRepository oOrganizationRepository = new OrganizationRepository();
		String sOrganizationId = oUser.getOrganizationId();

		// Find All the area belonging to the org
		AreaRepository oAreaRepository = new AreaRepository();
		List<Area> aoOrgAreas = oAreaRepository.getByOrganization(sOrganizationId);

		// We reuse the API call
		AreaResource oAreaResource = new AreaResource();

		for (Area oArea : aoOrgAreas) {
			oAreaResource.deleteArea(sSessionId, oArea.getId());
		}

		// Shall we do anything for subscriptions?

		// delete users of the org
		List<User> aoUsers = oUserRepository.getUsersByOrganizationId(sOrganizationId);
		for (User oOrgUser : aoUsers) {
			oUserRepository.deleteByUserId(oOrgUser.getUserId());
		}

		// delete org and otp
		oOrganizationRepository.delete(sOrganizationId);

	}

}
