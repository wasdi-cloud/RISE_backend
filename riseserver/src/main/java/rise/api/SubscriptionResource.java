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
import rise.lib.business.PaymentType;
import rise.lib.business.Plugin;
import rise.lib.business.Subscription;
import rise.lib.business.SubscriptionType;
import rise.lib.business.User;
import rise.lib.config.RiseConfig;
import rise.lib.config.StripeProductConfig;
import rise.lib.data.PluginRepository;
import rise.lib.data.SubscriptionRepository;
import rise.lib.data.SubscriptionTypeRepository;
import rise.lib.utils.PermissionsUtils;
import rise.lib.utils.Utils;
import rise.lib.utils.date.DateUtils;
import rise.lib.utils.log.RiseLog;
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
    		if (bValid == null) bValid = true;
    		
    		// Get the subscriptions of this org
    		SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();
    		List<Subscription> aoSubscriptions = oSubscriptionRepository.getSubscriptionsByOrganizationId(oUser.getOrganizationId());
    		
    		// Create VM list
			ArrayList<SubscriptionListViewModel> aoSubscriptionsVM = new ArrayList<>();
			
			// Convert the entities
			for (Subscription oSubscription : aoSubscriptions) {
				
				// Valid = false => we get all. Valid = true => only valid ones
				if (!bValid || oSubscription.isValid()) {
					SubscriptionListViewModel oListItem = (SubscriptionListViewModel) RiseViewModel.getFromEntity(SubscriptionListViewModel.class.getName(), oSubscription);
					aoSubscriptionsVM.add(oListItem);
				}
			}
			
			// return the list to the client
			return Response.ok(aoSubscriptionsVM).build();
    	}
		catch (Exception oEx) {
			RiseLog.errorLog("SubscriptionResource.getList: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	/**
	 * Get a Subscription by id
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
    		
    		if (oSubscription==null) {
				RiseLog.warnLog("SubscriptionResource.getById: subscription " + sId + " not found");
				return Response.status(Status.BAD_REQUEST).build();    			
    		}
    		
    		SubscriptionViewModel oSubscriptionViewModel = (SubscriptionViewModel) RiseViewModel.getFromEntity(SubscriptionViewModel.class.getName(), oSubscription);
			
			// return the list to the client
			return Response.ok(oSubscriptionViewModel).build();
    	}
		catch (Exception oEx) {
			RiseLog.errorLog("SubscriptionResource.getById: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	/**
	 * Updates a Subscription
	 * @param sSessionId
	 * @param oSubscriptionViewModel
	 * @return
	 */
	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	public Response update(@HeaderParam("x-session-token") String sSessionId, SubscriptionViewModel oSubscriptionViewModel) {
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
    		
    		// We do not want to be cheated: the buy date, exire date and valid flag DOES NOT come from the client!
    		oSubscription.setCreationDate(oFromDbSub.getCreationDate());
    		oSubscription.setBuyDate(oFromDbSub.getBuyDate());
    		oSubscription.setExpireDate(oFromDbSub.getExpireDate());
    		oSubscription.setValid(oFromDbSub.isValid());
    		
    		if (!oSubscriptionRepository.update(oSubscription, oSubscription.getId())) {
				RiseLog.warnLog("SubscriptionResource.update: There was an error updating the subscription");
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    		}
    		else {
    			return Response.ok().build();
    		}
		}
		catch (Exception oEx) {
			RiseLog.errorLog("SubscriptionResource.update: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}		
	}
	
	/**
	 * Compute the price of a Subscription
	 * @param oSubscription
	 * @return
	 */
	public double getSubscriptionPrice(Subscription oSubscription) {
		if (oSubscription == null) return -1;
		
		double dPrice = 0;
		
		PluginRepository oPluginRepository = new PluginRepository();
		
		for (String sPluginId : oSubscription.getPlugins()) {
			Plugin oPlugin = (Plugin) oPluginRepository.get(sPluginId);
			
			if (oPlugin == null) {
				RiseLog.warnLog("SubscriptionResource.getSubscriptionPrice: cannot find plugin " + sPluginId);
				continue;
			}
			
			if (oSubscription.isSupportsArchive()) {
				dPrice += oPlugin.getArchivePrice();
			}
			else {
				dPrice += oPlugin.getEmergencyPrice();
			}
		}
		
		return dPrice;
	}
	
	/**
	 * Get the price of a subscription
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
    		
    		if (oSubscriptionViewModel==null) {
				RiseLog.warnLog("SubscriptionResource.getPrice: sub view model null");
				return Response.status(Status.BAD_REQUEST).build();      			
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
	public Response buy(@HeaderParam("x-session-token") String sSessionId, SubscriptionViewModel oSubscriptionViewModel) {
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
    		
    		if (oSubscriptionViewModel==null) {
				RiseLog.warnLog("SubscriptionResource.getPrice: sub view model null");
				return Response.status(Status.BAD_REQUEST).build();      			
    		}
    		
    		
    		Subscription oSubscription = (Subscription) RiseViewModel.copyToEntity(Subscription.class.getName(), oSubscriptionViewModel);
    		oSubscription.setPrice(getSubscriptionPrice(oSubscription));
    		
    		double dNow = DateUtils.getNowAsDouble();
    		oSubscription.setCreationDate(dNow);
    		oSubscription.setBuyDate(null);
    		oSubscription.setValid(true);
    		oSubscription.setBuySuccess(false);
    		oSubscription.setId(Utils.getRandomName());

    		// Convert epoch to LocalDateTime for calendar-based arithmetic
    		LocalDateTime nowDateTime = LocalDateTime.ofEpochSecond((long) dNow / 1000, 0, ZoneOffset.UTC);
    		LocalDateTime expireDateTime;

    		if (oSubscription.getPaymentType().equals(PaymentType.MONTH)) {
    		    expireDateTime = nowDateTime.plusMonths(1); // Add 1 month
    		} else {
    		    expireDateTime = nowDateTime.plusYears(1); // Add 1 year
    		}

    		// Convert back to epoch milliseconds
    		double dExpire = expireDateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
    		oSubscription.setExpireDate(dExpire);
    		oSubscription.setOrganizationId(oUser.getOrganizationId());
    		
    		SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();
    		oSubscriptionRepository.add(oSubscription);
    		
    		SubscriptionViewModel oReturnVM = (SubscriptionViewModel) RiseViewModel.getFromEntity(SubscriptionViewModel.class.getName(), oSubscription);
			
    		return Response.ok(oReturnVM).build();
		}
		catch (Exception oEx) {
			RiseLog.errorLog("SubscriptionResource.buy: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} 
	}
	
	/**
	 * Generates the Stripe Payment Url
	 * @param sSessionId Session Id
	 * @param sSubscriptionId Subscription Id
	 * @param sWorkspaceId Workspace Id
	 * @return SuccessResponse with url in the message or ErrorResponse with the code of the error 
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
				RiseLog.debugLog("SubscriptionResource.getStripePaymentUrl: the subscription does not have a valid type, aborting");
				return Response.status(Status.BAD_REQUEST).build();
			}

			List<StripeProductConfig> aoProductConfigList = RiseConfig.Current.stripe.products;

			Map<String, String> aoProductConfigMap = aoProductConfigList.stream()
					.collect(Collectors.toMap(t -> t.id, t -> t.url));

			/*SubscriptionType oSubscriptionType = SubscriptionType.get(sSubscriptionType);

			if (oSubscriptionType == null) {
				RiseLog.warnLog("SubscriptionResource.getStripePaymentUrl: the subscription does not have a valid type, aborting");
				return Response.status(Status.BAD_REQUEST).build();
			} 
			else {*/
				String sBaseUrl = aoProductConfigMap.get(sSubscriptionType);

				if (Utils.isNullOrEmpty(sBaseUrl)) {
					RiseLog.debugLog("SubscriptionResource.getStripePaymentUrl: the config does not contain a valid configuration for the subscription");
					return Response.serverError().build();
				} 
				else {
					String sUrl = sBaseUrl + "?client_reference_id=" + sSubscriptionId;

					return Response.ok(sUrl).build();
				}
			//}
		} 
		catch (Exception oEx) {
			RiseLog.errorLog("SubscriptionResource.getStripePaymentUrl error " + oEx);
			return Response.serverError().build();
		}
	}
	
	/**
	 * Confirms the subscription after the successful transaction from Stripe
	 * The API should be called from Stripe. It connects again to Stripe to verify that the info is correct
	 * If all is fine it activates the subscription
	 * 
	 * @param sCheckoutSessionId Secret Checkout code used to link the stripe payment with the subscription
	 * @return
	 */
	@GET
	@Path("/stripe/confirmation/{CHECKOUT_SESSION_ID}")
	@Produces({"application/json", "application/xml", "text/xml" })
	public String confirmation(@PathParam("CHECKOUT_SESSION_ID") String sCheckoutSessionId) {
		RiseLog.debugLog("SubscriptionResource.confirmation( sCheckoutSessionId: " + sCheckoutSessionId + ")");

		if (Utils.isNullOrEmpty(sCheckoutSessionId)) {
			RiseLog.warnLog("SubscriptionResource.confirmation: Stripe returned a null CHECKOUT_SESSION_ID, aborting");
			return null;
		}

		try {
			StripeService oStripeService = new StripeService();
			StripePaymentDetail oStripePaymentDetail = oStripeService.retrieveStripePaymentDetail(sCheckoutSessionId);

			String sClientReferenceId = oStripePaymentDetail.getClientReferenceId();

			if (oStripePaymentDetail == null || Utils.isNullOrEmpty(sClientReferenceId)) {
				RiseLog.warnLog("SubscriptionResource.confirmation: Stripe returned an invalid result, aborting");
				return null;
			}

			String sSubscriptionId = null;
			//String sWorkspaceId = null;

			if (sClientReferenceId.contains("_")) {
				String[] asClientReferenceId = sClientReferenceId.split("_");
				sSubscriptionId = asClientReferenceId[0];
				//sWorkspaceId = asClientReferenceId[1];
			} else {
				sSubscriptionId = sClientReferenceId;
			}

			RiseLog.debugLog("SubscriptionResource.confirmation( sSubscriptionId: " + sSubscriptionId + ")");

			if (oStripePaymentDetail != null) {

				SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();

				Subscription oSubscription = (Subscription) oSubscriptionRepository.get(sSubscriptionId);

				if (oSubscription == null) {
					RiseLog.debugLog("SubscriptionResource.confirmation: subscription does not exist");
				} 
				else {
					oSubscription.setBuyDate(DateUtils.nowInMillis());
					oSubscription.setBuySuccess(true);

					oSubscriptionRepository.update(oSubscription,oSubscription.getId());
				}
			}		
		}
		catch (Exception oEx) {
			RiseLog.errorLog("SubscriptionResource.confirmation error " + oEx);
		}


		String sHtmlContent = "<script type=\"text/javascript\">\r\n" + 
				"setTimeout(\r\n" + 
				"function ( )\r\n" + 
				"{\r\n" + 
				"  self.close();\r\n" + 
				"}, 1000 );\r\n" + 
				"</script>";
		
		return sHtmlContent;
	}
}
