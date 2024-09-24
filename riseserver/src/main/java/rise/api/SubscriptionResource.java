package rise.api;

import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import rise.Rise;
import rise.lib.business.PaymentType;
import rise.lib.business.Plugin;
import rise.lib.business.Subscription;
import rise.lib.business.SubscriptionType;
import rise.lib.business.User;
import rise.lib.business.UserRole;
import rise.lib.data.PluginRepository;
import rise.lib.data.SubscriptionRepository;
import rise.lib.data.SubscriptionTypeRepository;
import rise.lib.utils.PermissionsUtils;
import rise.lib.utils.Utils;
import rise.lib.utils.date.DateUtils;
import rise.lib.utils.log.RiseLog;
import rise.lib.viewmodels.RiseViewModel;
import rise.lib.viewmodels.SubscriptionListViewModel;
import rise.lib.viewmodels.SubscriptionTypeViewModel;
import rise.lib.viewmodels.SubscriptionViewModel;

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
				if (bValid == false || oSubscription.isValid()) {
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
    		oSubscription.setBuyDate(oFromDbSub.getBuyDate());
    		oSubscription.setExpireDate(oFromDbSub.getExpireDate());
    		oSubscription.setValid(oFromDbSub.isValid());
    		
    		if (!oSubscriptionRepository.update(oSubscription)) {
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
    		oSubscription.setBuyDate(dNow);
    		oSubscription.setValid(true);
    		oSubscription.setId(Utils.getRandomName());
    		
    		double dExpire = dNow;
    		if (oSubscription.getPaymentType().equals(PaymentType.MONTH)) {
    			dExpire += 30*24*60*60*1000;
    		}
    		else {
    			dExpire += 365*24*60*60*1000;
    		}
    		
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
}
