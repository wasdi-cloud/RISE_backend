package rise.lib.utils;

import java.util.List;

import rise.lib.business.Area;
import rise.lib.business.Subscription;
import rise.lib.business.User;
import rise.lib.business.UserRole;
import rise.lib.data.AreaRepository;
import rise.lib.data.SubscriptionRepository;
import rise.lib.utils.date.DateUtils;

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
		
		if (isRiseAdmin(oUser)) return true;
		if (isAdmin(oUser)) return true;
		if (oUser.getRole().equals(UserRole.HQ)) return true;
		
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
	
	public static boolean canUserAccessArea(String sAreaId, User oUser) {
		AreaRepository oAreaRepository = new AreaRepository();
		Area oArea = (Area) oAreaRepository.get(sAreaId);
		return canUserAccessArea(oArea);
	}
	
	public static boolean canUserAccessArea(Area oArea, User oUser) {
		if (oArea == null) return false;
		
		//TODO
		
		return true;
	}
}
