package rise.api;

import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import rise.Rise;
import rise.lib.business.Subscription;
import rise.lib.business.User;
import rise.lib.business.UserRole;
import rise.lib.data.SubscriptionRepository;
import rise.lib.utils.Utils;
import rise.lib.utils.log.RiseLog;
import rise.lib.viewmodels.RiseViewModel;
import rise.lib.viewmodels.SubscriptionListViewModel;
import rise.lib.viewmodels.SubscriptionViewModel;

@Path("subscriptions")
public class SubscriptionResource {

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
    		if (!oUser.getRole().equals(UserRole.ADMIN)) {
				RiseLog.warnLog("SubscriptionResource.getList: not an admin");
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
	
	@GET
	@Path("byId")
	public Response getById(@HeaderParam("x-session-token") String sSessionId, @QueryParam("id") String sId) {
		
		try {
			// Check the session
			User oUser = Rise.getUserFromSession(sSessionId);
			
    		if (oUser == null) {
				RiseLog.warnLog("SubscriptionResource.getById: invalid Session");
				return Response.status(Status.UNAUTHORIZED).build();    			
    		}
    		
    		// We need an admin here!
    		if (!oUser.getRole().equals(UserRole.ADMIN)) {
				RiseLog.warnLog("SubscriptionResource.getById: not an admin");
				return Response.status(Status.UNAUTHORIZED).build();      			
    		}
    		
    		if (Utils.isNullOrEmpty(sId)) {
				RiseLog.warnLog("SubscriptionResource.getById: id null");
				return Response.status(Status.BAD_REQUEST).build();      			    			
    		}
    		
    		// Get the subscriptions of this org
    		SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();
    		Subscription oSubscription = (Subscription) oSubscriptionRepository.get(sId);
    		
    		SubscriptionViewModel oSubscriptionViewModel = (SubscriptionViewModel) RiseViewModel.getFromEntity(SubscriptionViewModel.class.getName(), oSubscription);
			
			// return the list to the client
			return Response.ok(oSubscriptionViewModel).build();
    	}
		catch (Exception oEx) {
			RiseLog.errorLog("SubscriptionResource.getById: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}	
}
