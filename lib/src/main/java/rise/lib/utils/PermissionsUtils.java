package rise.lib.utils;

import java.util.HashMap;
import java.util.List;

import rise.lib.business.Area;
import rise.lib.business.Organization;
import rise.lib.business.ResourceTypes;
import rise.lib.business.Subscription;
import rise.lib.business.SubscriptionType;
import rise.lib.business.User;
import rise.lib.business.UserResourcePermission;
import rise.lib.business.UserRole;
import rise.lib.data.AreaRepository;
import rise.lib.data.OrganizationRepository;
import rise.lib.data.SubscriptionRepository;
import rise.lib.data.SubscriptionTypeRepository;
import rise.lib.data.UserResourcePermissionRepository;
import rise.lib.utils.date.DateUtils;
import rise.lib.utils.log.RiseLog;

/**
 * Permissions Utility methods
 */
public class PermissionsUtils {
	
	/**
	 * Verify if the user is a RISE admin
	 * @param oUser
	 * @return
	 */
	public static boolean isRiseAdmin(User oUser) {
		if (oUser == null) return false;
		return UserRole.isRiseAdmin(oUser);
	}
	
	/**
	 * Verify if the user is an Organization Admin
	 * @param oUser
	 * @return
	 */
	public static boolean isAdmin(User oUser) {
		if (oUser == null) return false;
		return UserRole.isAdmin(oUser);
	}	
	
	/**
	 * Verify if the user can play the role of HQ
	 * @param oUser
	 * @return
	 */
	public static boolean hasHQRights(User oUser) {
		if (oUser == null) return false;
		if (oUser.getRole() == null) return false;
		
		if (isRiseAdmin(oUser)) return true;
		if (isAdmin(oUser)) return true;
		if (oUser.getRole().equals(UserRole.HQ)) return true;
		
		return false;
	}
	/**
	 * Verify if the user is a field operator
	 * @param oUser
	 * @return
	 */
	public static boolean hasFieldRights(User oUser) {
		if (oUser == null) return false;
		if (oUser.getRole() == null) return false;
		if (oUser.getRole().equals(UserRole.FIELD)) return true;
		return false;
	}
	
	
	
	/**
	 * Verify if the user has a valid subscription
	 * @param oUser
	 * @return
	 */
	public static boolean hasValidSubscription(User oUser) {
		if (oUser == null) return false;
		if (Utils.isNullOrEmpty(oUser.getUserId())) return false;
		if (Utils.isNullOrEmpty(oUser.getOrganizationId())) return false;
		
		SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();
		List<Subscription> aoSubscriptions = oSubscriptionRepository.getSubscriptionsByOrganizationId(oUser.getOrganizationId());
		if (aoSubscriptions==null) return false;
		if (aoSubscriptions.size()<=0) return false;
		
		for (Subscription oSubscription : aoSubscriptions) {
			if (oSubscription.isValid()) {
				double dNow = DateUtils.getNowAsDouble();
				if (oSubscription.getExpireDate()>dNow) return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Get a valid subscription for the user
	 * @param oUser
	 * @return
	 */
	public static Subscription getValidSubscription(User oUser) {
		if (oUser == null) return null;
		if (Utils.isNullOrEmpty(oUser.getUserId())) return null;
		if (Utils.isNullOrEmpty(oUser.getOrganizationId())) return null;
		
		SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();
		List<Subscription> aoSubscriptions = oSubscriptionRepository.getSubscriptionsByOrganizationId(oUser.getOrganizationId());
		if (aoSubscriptions==null) return null;
		if (aoSubscriptions.size()<=0) return null;
		
		for (Subscription oSubscription : aoSubscriptions) {
			if (oSubscription.isValid()) {
				double dNow = DateUtils.getNowAsDouble();
				if (oSubscription.getExpireDate()>dNow) return oSubscription;
			}
		}
		
		return null;
	}
	
	/**
	 * Get a valid subscription for the user
	 * @param oUser
	 * @return
	 */
	public static boolean canUserAddArea(User oUser) {
		// Check the user 
		if (oUser == null) return false;
		if (Utils.isNullOrEmpty(oUser.getUserId())) return false;
		if (Utils.isNullOrEmpty(oUser.getOrganizationId())) return false;
		
		// Get the list of subscriptions
		SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();
		List<Subscription> aoSubscriptions = oSubscriptionRepository.getSubscriptionsByOrganizationId(oUser.getOrganizationId());
		if (aoSubscriptions==null) return false;
		if (aoSubscriptions.size()<=0) return false;
				
		// Here we will keep the number of subscriptions the user purchased
		int iAvalableSubscriptions = 0;
		
		// We will need the types, use dictionary for optimization
		SubscriptionTypeRepository oSubscriptionTypeRepository = new SubscriptionTypeRepository();
		HashMap<String, SubscriptionType> aoSubTypes = new HashMap<String, SubscriptionType>();

		// For each
		for (Subscription oSubscription : aoSubscriptions) {
			// Must be valid
			if (oSubscription.isValid()) {
				// And not expired
				double dNow = DateUtils.getNowAsDouble();
				if (oSubscription.getExpireDate()>dNow) {
					
					// Do we have already this type?
					if (aoSubTypes.containsKey(oSubscription.getType()) == false) {
						// No, add it to the db
						SubscriptionType oSubType = oSubscriptionTypeRepository.getByType(oSubscription.getType());
						if (oSubType == null) {
							RiseLog.warnLog("PermissionUtils.canUserAddArea: the subscription " + oSubscription.getId() + " has a not recognized type " + oSubscription.getType());
							continue;
						}
						
						// Ok add it the the dictionary
						aoSubTypes.put(oSubscription.getType(), oSubType);
					}
					
					// Must be available if we are here
					SubscriptionType oSubType = aoSubTypes.get(oSubscription.getType());
					
					if (oSubType==null) continue;
					
					// We can sum the number of allowed areas
					iAvalableSubscriptions += oSubType.getAllowedAreas();
				};
			}
		}
		
		AreaRepository oAreaRepository = new AreaRepository();
		List<Area> aoAreas = oAreaRepository.getByOrganization(oUser.getOrganizationId());
		
		// The number of areas cannot exceed the limit
		if (aoAreas.size()>=iAvalableSubscriptions) return false;
		else return true;
		
	}
	
	public static boolean canUserAccessArea(String sAreaId, User oUser) {
		AreaRepository oAreaRepository = new AreaRepository();
		Area oArea = (Area) oAreaRepository.get(sAreaId);
		return canUserAccessArea(oArea, oUser);
	}
	
	/**
	 * Verify if an user can access an area
	 * @param oArea
	 * @param oUser
	 * @return
	 */
	public static boolean canUserAccessArea(Area oArea, User oUser) {
		// Safe check
		if (oArea == null) return false;
		if (oUser == null) return false;
		
		// Public Area are ok
		if (oArea.isPublicArea()) return true;
		
		// If it is not public, must be for sure same org
		if (!oArea.getOrganizationId().equals(oUser.getOrganizationId())) {
			// It is not: is the area shared with the user?
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			
			// Search the permission
			UserResourcePermission oPermission = oUserResourcePermissionRepository.getPermissionByTypeUserIdResourceId(ResourceTypes.AREA.name(),oUser.getUserId(), oArea.getId());
			
			if (oPermission != null)  {
				// Valid! can see it
				return true;
			}
			else {
				// Nothing, not your area dude
				return false;	
			}
		}
		else {
			// If it is in the same org and is admin or HQ can see it
			if (hasHQRights(oUser)) {
				return true;
			}
			else if (hasFieldRights(oUser)) {
				// If instead is field user We need to check if is assigned to this area
				if (oArea.getFieldOperators() != null) {
					if (oArea.getFieldOperators().contains(oUser.getUserId())) {
						return true;
					}
				}
			}
		}
				
		// If we arrive here, is not your area dude!
		return false;
	}
	
	public static boolean canUserWriteArea(Area oArea, User oUser) {
		// Safe check
		if (oArea == null) return false;
		if (oUser == null) return false;
		
		// If it is not public, must be for sure same org
		if (!oArea.getOrganizationId().equals(oUser.getOrganizationId())) {
			// It is not: is the area shared with the user?
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			
			// Search the permission
			UserResourcePermission oPermission = oUserResourcePermissionRepository.getPermissionByTypeUserIdResourceId(ResourceTypes.AREA.name(),oUser.getUserId(), oArea.getId());
			
			if (oPermission != null)  {
				if (oPermission.canWrite()) return true;
			}
			
			return false;		
		}
		else {
			// If it is in the same org and is admin or HQ can see it
			if (hasHQRights(oUser)) {
				return true;
			}
		}
				
		// If we arrive here, is not your area dude!
		return false;
	}
		
	
	public static boolean canUserAccessOrganization(String sOrganizationId, User oUser) {
		OrganizationRepository oOrganizationRepository = new OrganizationRepository();
		Organization oOrganization = (Organization) oOrganizationRepository.get(sOrganizationId);
		return canUserAccessOrganization(oOrganization, oUser);
	}
	
	public static boolean canUserAccessOrganization(Organization oOrganization, User oUser) {
		if (oOrganization == null) return false;
		if (oUser == null) return false;
		
		if (oUser.getOrganizationId().equals(oOrganization.getId())) return true;
		
		return false;
	}
}
