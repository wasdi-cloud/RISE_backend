package rise.lib.viewmodels;

import rise.lib.business.UserRole;

public class UserViewModel extends RiseViewModel {
	public String userId;
	
	public String email;
	
	public String name;
	
	public String surname;
	
	public String mobile;
	
	public String internationalPrefix;
	
	public UserRole role;
	
	public boolean acceptedTermsAndConditions;
	
	public boolean acceptedPrivacy;
	
	public Double lastLoginDate;
	
	public boolean notifyNewsletter;
	
	public boolean notifyMaintenance;
	
	public boolean notifyActivities;
	
	public String defaultLanguage;
	
	public String organizationId;
}
