package rise.lib.business;

import rise.lib.utils.Utils;

public enum UserRole {
	RISE_ADMIN("RISE_ADMIN"),
	ADMIN("ADMIN"),
	HQ("HQ"),
	FIELD("FIELD");
	
	private final String value;
	
	UserRole(String sValue) {
		this.value = sValue;
	}

	public String getString() {
		return value;
	}		
	
	public static boolean isValid(String sValue) {
		if (Utils.isNullOrEmpty(sValue)) return false;
		
		if (sValue.equals(RISE_ADMIN.getString())) return true;
		if (sValue.equals(ADMIN.getString())) return true;
		if (sValue.equals(HQ.getString())) return true;
		if (sValue.equals(FIELD.getString())) return true;
		
		return false;
	}
	
//	public static boolean isAdmin(String sUserId) {
//		if (Utils.isNullOrEmpty(sUserId)) return false;
//		
//		UserRepository oUserRepository = new UserRepository();
//		User oUser = oUserRepository.getUser(sUserId);
//		
//		return isAdmin(oUser);
//	}
	
	public static boolean isAdmin(User oUser) {
		if (oUser == null) return false;
		
		if (!isValid(oUser.getRole().getString())) return false;
		
		if (oUser.getRole().equals(ADMIN)) return true;
		
		return false;
	}
	
	public static boolean isRiseAdmin(User oUser) {
		if (oUser == null) return false;
		
		if (!isValid(oUser.getRole().getString())) return false;
		
		if (oUser.getRole().equals(RISE_ADMIN)) return true;
		
		return false;
	}	
}
