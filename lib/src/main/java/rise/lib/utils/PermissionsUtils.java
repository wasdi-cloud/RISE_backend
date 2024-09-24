package rise.lib.utils;

import rise.lib.business.User;
import rise.lib.business.UserRole;

public class PermissionsUtils {
	
	public static boolean isRiseAdmin(User oUser) {
		if (oUser == null) return false;
		return UserRole.isRiseAdmin(oUser);
	}
	
	public static boolean isAdmin(User oUser) {
		if (oUser == null) return false;
		return UserRole.isAdmin(oUser);
	}	
	
	public static boolean hasHQRights(User oUser) {
		if (oUser == null) return false;
		
		if (isRiseAdmin(oUser)) return true;
		if (isAdmin(oUser)) return true;
		if (oUser.getRole().equals(UserRole.HQ)) return true;
		
		return false;
	}
	
	
}
