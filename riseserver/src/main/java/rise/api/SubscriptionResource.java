package rise.api;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import rise.Rise;
import rise.lib.business.Organization;
import rise.lib.business.PaymentType;
import rise.lib.business.Plugin;
import rise.lib.business.Subscription;
import rise.lib.business.SubscriptionType;
import rise.lib.business.User;
import rise.lib.config.RiseConfig;
import rise.lib.config.StripeProductConfig;
import rise.lib.data.OrganizationRepository;
import rise.lib.data.PluginRepository;
import rise.lib.data.SubscriptionRepository;
import rise.lib.data.SubscriptionTypeRepository;
import rise.lib.data.UserRepository;
import rise.lib.utils.PermissionsUtils;
import rise.lib.utils.Utils;
import rise.lib.utils.date.DateUtils;
import rise.lib.utils.i8n.LangUtils;
import rise.lib.utils.i8n.Languages;
import rise.lib.utils.i8n.StringCodes;
import rise.lib.utils.log.RiseLog;
import rise.lib.utils.mail.MailUtils;
import rise.lib.viewmodels.RiseViewModel;
import rise.lib.viewmodels.StripePaymentDetail;
import rise.lib.viewmodels.SubscriptionListViewModel;
import rise.lib.viewmodels.SubscriptionTypeViewModel;
import rise.lib.viewmodels.SubscriptionViewModel;
import rise.services.StripeService;

@Path("subscriptions")
public class SubscriptionResource {

	/**
	 * Get a list of Subscriptions per user
	 * 
	 * @param sSessionId
	 * @param bValid
	 * @return
	 */
	@GET
	@Path("list")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getList(@HeaderParam("x-session-token") String sSessionId, @QueryParam("valid") Boolean bValid) {

		try {
			// Check the session
			User oUser = Rise.getUserFromSession(sSessionId);

			if (oUser == null) {
				RiseLog.warnLog("SubscriptionResource.getList: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			// We need an admin here!
			if (!PermissionsUtils.hasHQRights(oUser)) {
				RiseLog.warnLog("SubscriptionResource.getList: cannot handle subscriptions");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			// valid true by default
			if (bValid == null)
				bValid = true;

			// Get the subscriptions of this org
			SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();
			List<Subscription> aoSubscriptions = oSubscriptionRepository
					.getSubscriptionsByOrganizationId(oUser.getOrganizationId());

			// Create VM list
			ArrayList<SubscriptionListViewModel> aoSubscriptionsVM = new ArrayList<>();

			// Convert the entities
			for (Subscription oSubscription : aoSubscriptions) {

				// Valid = false => we get all. Valid = true => only valid ones
				if (!bValid || oSubscription.isValid()) {
					SubscriptionListViewModel oListItem = (SubscriptionListViewModel) RiseViewModel
							.getFromEntity(SubscriptionListViewModel.class.getName(), oSubscription);
					aoSubscriptionsVM.add(oListItem);
				}
			}

			// return the list to the client
			return Response.ok(aoSubscriptionsVM).build();
		} catch (Exception oEx) {
			RiseLog.errorLog("SubscriptionResource.getList: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Get a Subscription by id
	 * 
	 * @param sSessionId
	 * @param sId
	 * @return
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getById(@HeaderParam("x-session-token") String sSessionId, @QueryParam("id") String sId) {

		try {
			// Check the session
			User oUser = Rise.getUserFromSession(sSessionId);

			if (oUser == null) {
				RiseLog.warnLog("SubscriptionResource.getById: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			// We need an admin here!
			if (!PermissionsUtils.hasHQRights(oUser)) {
				RiseLog.warnLog("SubscriptionResource.getById: not an HQ level");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			if (Utils.isNullOrEmpty(sId)) {
				RiseLog.warnLog("SubscriptionResource.getById: id null");
				return Response.status(Status.BAD_REQUEST).build();
			}

			// Get the subscription
			SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();
			Subscription oSubscription = (Subscription) oSubscriptionRepository.get(sId);

			if (oSubscription == null) {
				RiseLog.warnLog("SubscriptionResource.getById: subscription " + sId + " not found");
				return Response.status(Status.BAD_REQUEST).build();
			}

			SubscriptionViewModel oSubscriptionViewModel = (SubscriptionViewModel) RiseViewModel
					.getFromEntity(SubscriptionViewModel.class.getName(), oSubscription);

			// return the list to the client
			return Response.ok(oSubscriptionViewModel).build();
		} catch (Exception oEx) {
			RiseLog.errorLog("SubscriptionResource.getById: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Updates a Subscription
	 * 
	 * @param sSessionId
	 * @param oSubscriptionViewModel
	 * @return
	 */
	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	public Response update(@HeaderParam("x-session-token") String sSessionId,
			SubscriptionViewModel oSubscriptionViewModel) {
		try {
			// Check the session
			User oUser = Rise.getUserFromSession(sSessionId);

			if (oUser == null) {
				RiseLog.warnLog("SubscriptionResource.update: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			// We need an admin here!
			if (!PermissionsUtils.hasHQRights(oUser)) {
				RiseLog.warnLog("SubscriptionResource.update: not an HQ level");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			if (oSubscriptionViewModel == null) {
				RiseLog.warnLog("SubscriptionResource.update: Subscription null");
				return Response.status(Status.BAD_REQUEST).build();
			}

			if (Utils.isNullOrEmpty(oSubscriptionViewModel.id)) {
				RiseLog.warnLog("SubscriptionResource.update: Subscription id null");
				return Response.status(Status.BAD_REQUEST).build();
			}

			// Check if we have this subscription
			SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();
			Subscription oFromDbSub = (Subscription) oSubscriptionRepository.get(oSubscriptionViewModel.id);

			if (oFromDbSub == null) {
				RiseLog.warnLog("SubscriptionResource.update: Subscription with this id " + oSubscriptionViewModel.id + " not found");
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			// Create the updated entity
			Subscription oSubscription = (Subscription) RiseViewModel.copyToEntity(Subscription.class.getName(), oSubscriptionViewModel);

			// We do not want to be cheated: the buy date, exire date and valid flag DOES
			// NOT come from the client!
			oSubscription.setCreationDate(oFromDbSub.getCreationDate());
			oSubscription.setBuyDate(oFromDbSub.getBuyDate());
			oSubscription.setExpireDate(oFromDbSub.getExpireDate());
			oSubscription.setValid(oFromDbSub.isValid());
			oSubscription.setBuySuccess(oFromDbSub.isBuySuccess());

			if (!oSubscriptionRepository.update(oSubscription, oSubscription.getId())) {
				RiseLog.warnLog("SubscriptionResource.update: There was an error updating the subscription");
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			} else {
				return Response.ok().build();
			}
		} catch (Exception oEx) {
			RiseLog.errorLog("SubscriptionResource.update: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Compute the price of a Subscription
	 * 
	 * @param oSubscription
	 * @return
	 */
	public double getSubscriptionPrice(Subscription oSubscription) {
		if (oSubscription == null)
			return -1;
		
		SubscriptionTypeRepository oSubscriptionTypeRepository = new SubscriptionTypeRepository();
		SubscriptionType oSubscriptionType = oSubscriptionTypeRepository.getByType(oSubscription.getType());
		if (oSubscriptionType == null) return -1.0;

		double dPrice = oSubscriptionType.getPrice();

		PluginRepository oPluginRepository = new PluginRepository();

		for (String sPluginId : oSubscription.getPlugins()) {
			Plugin oPlugin = (Plugin) oPluginRepository.get(sPluginId);

			if (oPlugin == null) {
				RiseLog.warnLog("SubscriptionResource.getSubscriptionPrice: cannot find plugin " + sPluginId);
				continue;
			}

			if (oSubscription.isSupportsArchive()) {
				dPrice += oPlugin.getArchivePrice();
			} else {
				dPrice += oPlugin.getEmergencyPrice();
			}
		}

		return dPrice;
	}

	/**
	 * Get the price of a subscription
	 * 
	 * @param sSessionId
	 * @param oSubscriptionViewModel
	 * @return
	 */
	@POST
	@Path("price")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPrice(@HeaderParam("x-session-token") String sSessionId, SubscriptionViewModel oSubscriptionViewModel) {
		try {
			// Check the session
			User oUser = Rise.getUserFromSession(sSessionId);

			if (oUser == null) {
				RiseLog.warnLog("SubscriptionResource.getPrice: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			// We need an admin here!
			if (!PermissionsUtils.hasHQRights(oUser)) {
				RiseLog.warnLog("SubscriptionResource.getPrice: not an HQ level");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			if (oSubscriptionViewModel == null) {
				RiseLog.warnLog("SubscriptionResource.getPrice: sub view model null");
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			if (oSubscriptionViewModel.paymentType.equals(PaymentType.MONTH)) {
				oSubscriptionViewModel.type = oSubscriptionViewModel.type + "_QUARTER"; 
			}

			Subscription oSubscription = (Subscription) RiseViewModel.copyToEntity(Subscription.class.getName(), oSubscriptionViewModel);

			oSubscriptionViewModel.price = getSubscriptionPrice(oSubscription);

			return Response.ok(oSubscriptionViewModel).build();
		} 
		catch (Exception oEx) {
			RiseLog.errorLog("SubscriptionResource.getPrice: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Get the list of types of subscrptions
	 * 
	 * @param sSessionId
	 * @return
	 */
	@GET
	@Path("types")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTypes(@HeaderParam("x-session-token") String sSessionId) {
		try {
			// Check the session
			User oUser = Rise.getUserFromSession(sSessionId);

			if (oUser == null) {
				RiseLog.warnLog("SubscriptionResource.getTypes: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			// We need an admin here!
			if (!PermissionsUtils.hasHQRights(oUser)) {
				RiseLog.warnLog("SubscriptionResource.getTypes: not an HQ level");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			SubscriptionTypeRepository oSubscriptionTypeRepository = new SubscriptionTypeRepository();
			List<SubscriptionType> aoTypes = oSubscriptionTypeRepository.getAll();

			ArrayList<SubscriptionTypeViewModel> aoTypeViewModels = new ArrayList<>();

			for (SubscriptionType oType : aoTypes) {
				
				if (oType.getStringCode().contains("_QUARTER")) continue;
				
				SubscriptionTypeViewModel oSubscriptionTypesViewModel = (SubscriptionTypeViewModel) RiseViewModel.getFromEntity(SubscriptionTypeViewModel.class.getName(), oType);
				aoTypeViewModels.add(oSubscriptionTypesViewModel);
			}

			return Response.ok(aoTypeViewModels).build();
		} 
		catch (Exception oEx) {
			RiseLog.errorLog("SubscriptionResource.getTypes: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public Response createSubscription(@HeaderParam("x-session-token") String sSessionId,
			SubscriptionViewModel oSubscriptionViewModel) {
		try {

			// Check the session
			User oUser = Rise.getUserFromSession(sSessionId);

			if (oUser == null) {
				RiseLog.warnLog("SubscriptionResource.getPrice: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			// We need an admin here!
			if (!PermissionsUtils.hasHQRights(oUser)) {
				RiseLog.warnLog("SubscriptionResource.getPrice: not an HQ level");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			if (oSubscriptionViewModel == null) {
				RiseLog.warnLog("SubscriptionResource.getPrice: sub view model null");
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			if (oSubscriptionViewModel.paymentType.equals(PaymentType.MONTH)) {
				oSubscriptionViewModel.type = oSubscriptionViewModel.type + "_QUARTER"; 
			}			

			Subscription oSubscription = (Subscription) RiseViewModel.copyToEntity(Subscription.class.getName(), oSubscriptionViewModel);
			oSubscription.setPrice(getSubscriptionPrice(oSubscription));

			double dNow = DateUtils.getNowAsDouble();
			oSubscription.setCreationDate(dNow);
			oSubscription.setBuyDate(null);
			oSubscription.setExpireDate(null);
			oSubscription.setValid(false);
			oSubscription.setBuySuccess(false);
			oSubscription.setId(Utils.getRandomName());
			oSubscription.setPaymentMethod(oSubscriptionViewModel.paymentMethod);

			
			oSubscription.setOrganizationId(oUser.getOrganizationId());

			SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();
			oSubscriptionRepository.add(oSubscription);

			SubscriptionViewModel oReturnVM = (SubscriptionViewModel) RiseViewModel
					.getFromEntity(SubscriptionViewModel.class.getName(), oSubscription);
			//Here , we check if the payment method is wire and we send email to wasdi team informing them about this 
			if(oSubscriptionViewModel.paymentMethod.equals("wire")) {
				OrganizationRepository oOrganizationRepository=new OrganizationRepository();
				Organization oOrg=oOrganizationRepository.getOrganization(oUser.getOrganizationId());
				String sTitle = LangUtils.getLocalizedString(StringCodes.NOTIFICATIONS_WIRE_TRANSFER_REQUEST_TITLE.name(), Languages.EN.name());
				String sMessage = LangUtils.getLocalizedString(StringCodes.NOTIFICATIONS_WIRE_TRANSFER_REQUEST_MESSAGE.name(), Languages.EN.name());
				sMessage = sMessage.replace("%%USER_NAME%%", oUser.getName());
				sMessage = sMessage.replace("%%ORG_NAME%%", oOrg.getName());
				sMessage = sMessage.replace("%%SUB_NAME%%", oSubscription.getName());
				sMessage = sMessage.replace("%%SUB_ID%%", oSubscription.getId());
				
				MailUtils.sendEmail(RiseConfig.Current.notifications.wasdiAdminMail,sTitle, sMessage);
			}else if(oSubscriptionViewModel.paymentMethod.equals("contact")){
				//todo handle the use case of when user select contact option as payment method , we send him an email ..ect
			}
			return Response.ok(oReturnVM).build();
		} catch (Exception oEx) {
			RiseLog.errorLog("SubscriptionResource.buy: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Generates the Stripe Payment Url
	 * 
	 * @param sSessionId      Session Id
	 * @param sSubscriptionId Subscription Id
	 * @return SuccessResponse with url in the message or ErrorResponse with the
	 *         code of the error
	 */
	@GET
	@Path("stripe/paymentUrl")
	public Response getStripePaymentUrl(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("subscription") String sSubscriptionId) {

		RiseLog.debugLog("SubscriptionResource.getStripePaymentUrl( " + "Subscription: " + sSubscriptionId + ")");

		User oUser = Rise.getUserFromSession(sSessionId);

		if (oUser == null) {
			RiseLog.warnLog("SubscriptionResource.getStripePaymentUrl: invalid session");
			return Response.status(Status.UNAUTHORIZED).build();
		}

		try {
			// Domain Check
			if (Utils.isNullOrEmpty(sSubscriptionId)) {
				RiseLog.warnLog("SubscriptionResource.getStripePaymentUrl: invalid subscription id");
				return Response.status(Status.BAD_REQUEST).build();
			}

			if (!PermissionsUtils.hasHQRights(oUser)) {
				RiseLog.warnLog("SubscriptionResource.getStripePaymentUrl: user cannot access subscription info");
				return Response.status(Status.FORBIDDEN).build();
			}

			// Create repo
			SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();

			// Get requested subscription
			Subscription oSubscription = (Subscription) oSubscriptionRepository.get(sSubscriptionId);

			if (oSubscription == null) {
				RiseLog.warnLog("SubscriptionResource.getStripePaymentUrl: subscription does not exist");
				return Response.status(Status.BAD_REQUEST).build();
			}

			String sSubscriptionType = oSubscription.getType();

			if (Utils.isNullOrEmpty(sSubscriptionType)) {
				RiseLog.debugLog(
						"SubscriptionResource.getStripePaymentUrl: the subscription does not have a valid type, aborting");
				return Response.status(Status.BAD_REQUEST).build();
			}

			List<StripeProductConfig> aoProductConfigList = RiseConfig.Current.stripe.products;

			Map<String, String> aoProductConfigMap = aoProductConfigList.stream()
					.collect(Collectors.toMap(t -> t.id, t -> t.url));

			/*
			 * SubscriptionType oSubscriptionType = SubscriptionType.get(sSubscriptionType);
			 * 
			 * if (oSubscriptionType == null) { RiseLog.
			 * warnLog("SubscriptionResource.getStripePaymentUrl: the subscription does not have a valid type, aborting"
			 * ); return Response.status(Status.BAD_REQUEST).build(); } else {
			 */
			String sBaseUrl = aoProductConfigMap.get(sSubscriptionType);

			if (Utils.isNullOrEmpty(sBaseUrl)) {
				RiseLog.debugLog(
						"SubscriptionResource.getStripePaymentUrl: the config does not contain a valid configuration for the subscription");
				return Response.serverError().build();
			} else {
				String sUrl = sBaseUrl + "?client_reference_id=" + sSubscriptionId;

				return Response.ok(sUrl).build();
			}
			// }
		} catch (Exception oEx) {
			RiseLog.errorLog("SubscriptionResource.getStripePaymentUrl error " + oEx);
			return Response.serverError().build();
		}
	}

	/**
	 * Confirms the subscription after the successful transaction from Stripe The
	 * API should be called from Stripe. It connects again to Stripe to verify that
	 * the info is correct If all is fine it activates the subscription
	 * 
	 * @param sCheckoutSessionId Secret Checkout code used to link the stripe
	 *                           payment with the subscription
	 * @return
	 */
	@GET
	@Path("/stripe/confirmation/{CHECKOUT_SESSION_ID}")
	@Produces({ "application/json", "application/xml", "text/xml" })
	public Response confirmSubscriptionBuy(@PathParam("CHECKOUT_SESSION_ID") String sCheckoutSessionId) {
		RiseLog.debugLog("SubscriptionResource.confirmation( sCheckoutSessionId: " + sCheckoutSessionId + ")");

		if (Utils.isNullOrEmpty(sCheckoutSessionId)) {
			RiseLog.warnLog("SubscriptionResource.confirmation: Stripe returned a null CHECKOUT_SESSION_ID, aborting");
			return Response.status(Status.BAD_REQUEST).build();
		}

		try {
			StripeService oStripeService = new StripeService();
			StripePaymentDetail oStripePaymentDetail = oStripeService.retrieveStripePaymentDetail(sCheckoutSessionId);

			String sClientReferenceId = oStripePaymentDetail.getClientReferenceId();

			if (oStripePaymentDetail == null || Utils.isNullOrEmpty(sClientReferenceId)) {
				RiseLog.warnLog("SubscriptionResource.confirmation: Stripe returned an invalid result, aborting");
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}

			String sSubscriptionId = null;
			// String sWorkspaceId = null;

			if (sClientReferenceId.contains("_")) {
				String[] asClientReferenceId = sClientReferenceId.split("_");
				sSubscriptionId = asClientReferenceId[0];
				// sWorkspaceId = asClientReferenceId[1];
			} else {
				sSubscriptionId = sClientReferenceId;
			}

			RiseLog.debugLog("SubscriptionResource.confirmation( sSubscriptionId: " + sSubscriptionId + ")");

			if (oStripePaymentDetail != null) {

				SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();

				Subscription oSubscription = (Subscription) oSubscriptionRepository.get(sSubscriptionId);

				if (oSubscription == null) {
					RiseLog.debugLog("SubscriptionResource.confirmation: subscription does not exist");
					return Response.status(Status.BAD_REQUEST).build();
				} else {
					
					//check if its already paid 
					if(oSubscription.isBuySuccess()) {
						RiseLog.debugLog("SubscriptionResource.confirmation: subscription already paid");
						return Response.status(Status.BAD_REQUEST).build();
					}
					double dNow = DateUtils.getNowAsDouble();
					oSubscription.setBuyDate(dNow);
					oSubscription.setBuySuccess(true);
					oSubscription.setValid(true);
					oSubscription.setStripeInvoicePdfUrl(oStripePaymentDetail.getInvoicePdfUrl());
					// Convert epoch to LocalDateTime for calendar-based arithmetic
					LocalDateTime oNowDateTime = LocalDateTime.ofEpochSecond((long) dNow / 1000, 0, ZoneOffset.UTC);
					LocalDateTime oExpireDateTime;

					if (oSubscription.getPaymentType().equals(PaymentType.MONTH)) {
						oExpireDateTime = oNowDateTime.plusMonths(1); // Add 1 month
					} else {
						oExpireDateTime = oNowDateTime.plusYears(1); // Add 1 year
					}

					// Convert back to epoch milliseconds
					double dExpire = oExpireDateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
					oSubscription.setExpireDate(dExpire);
					oSubscriptionRepository.update(oSubscription, oSubscription.getId());
					// send email to the user telling him his sub is successful

					// We replace the sub name, org name  and the expiry date in the message
					OrganizationRepository oOrganizationRepository=new OrganizationRepository();
					Organization oOrg=(Organization)oOrganizationRepository.get(oSubscription.getOrganizationId());
					String sOrgName=oOrg.getName();
					UserRepository oUserRepository=new UserRepository();
					List<User> aoAdmins=oUserRepository.getAdminsOfOrganization(oOrg.getId());
					//And we send an email to the org admins !
					for (User oAdmin : aoAdmins) {
						String sUserLanguage;
						String sDefaultLanguageCode = oAdmin.getDefaultLanguage(); // Get the language code once

						try {
							if(Utils.isNullOrEmpty(sDefaultLanguageCode)){
								sUserLanguage = Languages.EN.name();
							}else{
								sUserLanguage = Languages.valueOf(sDefaultLanguageCode.toUpperCase()).name();
							}
						} catch (IllegalArgumentException e) {
							RiseLog.debugLog("SubscriptionResource.confirmSubscriptionBuy: Invalid language code '" + sDefaultLanguageCode + "' found. Defaulting to English.");
							sUserLanguage = Languages.EN.name();
						}
						String sTitle = LangUtils.getLocalizedString(StringCodes.NOTIFICATIONS_BUY_SUB_SUCCESS_TITLE.name(), sUserLanguage);
						String sMessage = LangUtils.getLocalizedString(StringCodes.NOTIFICATIONS_BUY_SUB_SUCCESS_MESSAGE.name(), sUserLanguage);
						sMessage = sMessage.replace("%%ORG_NAME%%", sOrgName);
						sMessage = sMessage.replace("%%SUB_NAME%%", oSubscription.getName());
						sMessage = sMessage.replace("%%EXPIRY_DATE%%", oExpireDateTime.toString());
						MailUtils.sendEmail(RiseConfig.Current.notifications.riseAdminMail, oAdmin.getEmail(), sTitle, sMessage, true);	
					}
				}
			}
		} catch (Exception oEx) {
			RiseLog.errorLog("SubscriptionResource.confirmation error " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}

		return Response.ok().build();
	}
	
	
	
	@GET
	@Path("stripe/invoice")
	public Response getInvoiceFromSubscription(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("subscription") String sSubscriptionId) {
		RiseLog.debugLog("SubscriptionResource.getInvoiceFromSubscription( " + "Subscription: " + sSubscriptionId + ")");

		User oUser = Rise.getUserFromSession(sSessionId);

		if (oUser == null) {
			RiseLog.warnLog("SubscriptionResource.getInvoiceFromSubscription: invalid session");
			return Response.status(Status.UNAUTHORIZED).build();
		}

		try {
			// Domain Check
			if (Utils.isNullOrEmpty(sSubscriptionId)) {
				RiseLog.warnLog("SubscriptionResource.getInvoiceFromSubscription: invalid subscription id");
				return Response.status(Status.BAD_REQUEST).build();
			}

			if (!PermissionsUtils.hasHQRights(oUser)) {
				RiseLog.warnLog("SubscriptionResource.getInvoiceFromSubscription: user cannot access subscription info");
				return Response.status(Status.FORBIDDEN).build();
			}

			// Create repo
			SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();

			// Get requested subscription
			Subscription oSubscription = (Subscription) oSubscriptionRepository.get(sSubscriptionId);

			if (oSubscription == null) {
				RiseLog.warnLog("SubscriptionResource.getInvoiceFromSubscription: subscription does not exist");
				return Response.status(Status.BAD_REQUEST).build();
			}
			return Response.ok(oSubscription.getStripeInvoicePdfUrl()).build();	
		} catch (Exception oEx) {
			RiseLog.errorLog("SubscriptionResource.getStripePaymentUrl error " + oEx);
			return Response.serverError().build();
		}
	}
}
