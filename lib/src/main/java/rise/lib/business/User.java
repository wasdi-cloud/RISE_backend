package rise.lib.business;

public class User extends RiseEntity {
	
	private String userId;
	
	private String email;
	
	private String name;
	
	private String surname;
	
	private String mobile;
	
	private String internationalPrefix;
	
	private UserRole role;
	
	private Double registrationDate;
	
	private Double confirmationDate;
	
	private boolean acceptedTermsAndConditions;
	
	private Double termsAndConditionAcceptedDate;
	
	private boolean acceptedPrivacy;
	
	private Double privacyAcceptedDate;
	
	private Double lastPasswordUpdateDate;
	
	private Double lastLoginDate;
	
	private Double lastResetPasswordRequest;
	
	private boolean notifyNewsletter;
	
	private boolean notifyMaintenance;
	
	private boolean notifyActivities;
	
	private String defaultLanguage;
	
	private String organizationId;
	
	private String confirmationCode;
	
	private String password;

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSurname() {
		return surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public Double getRegistrationDate() {
		return registrationDate;
	}

	public void setRegistrationDate(Double registrationDate) {
		this.registrationDate = registrationDate;
	}

	public Double getConfirmationDate() {
		return confirmationDate;
	}

	public void setConfirmationDate(Double confirmationDate) {
		this.confirmationDate = confirmationDate;
	}

	public boolean isAcceptedTermsAndConditions() {
		return acceptedTermsAndConditions;
	}

	public void setAcceptedTermsAndConditions(boolean acceptedTermsAndConditions) {
		this.acceptedTermsAndConditions = acceptedTermsAndConditions;
	}

	public Double getTermsAndConditionAcceptedDate() {
		return termsAndConditionAcceptedDate;
	}

	public void setTermsAndConditionAcceptedDate(Double termsAndConditionAcceptedDate) {
		this.termsAndConditionAcceptedDate = termsAndConditionAcceptedDate;
	}

	public boolean isAcceptedPrivacy() {
		return acceptedPrivacy;
	}

	public void setAcceptedPrivacy(boolean acceptedPrivacy) {
		this.acceptedPrivacy = acceptedPrivacy;
	}

	public Double getPrivacyAcceptedDate() {
		return privacyAcceptedDate;
	}

	public void setPrivacyAcceptedDate(Double privacyAcceptedDate) {
		this.privacyAcceptedDate = privacyAcceptedDate;
	}

	public Double getLastPasswordUpdateDate() {
		return lastPasswordUpdateDate;
	}

	public void setLastPasswordUpdateDate(Double lastPasswordUpdateDate) {
		this.lastPasswordUpdateDate = lastPasswordUpdateDate;
	}

	public Double getLastLoginDate() {
		return lastLoginDate;
	}

	public void setLastLoginDate(Double lastLoginDate) {
		this.lastLoginDate = lastLoginDate;
	}

	public Double getLastResetPasswordRequest() {
		return lastResetPasswordRequest;
	}

	public void setLastResetPasswordRequest(Double lastResetPasswordRequest) {
		this.lastResetPasswordRequest = lastResetPasswordRequest;
	}

	public boolean isNotifyNewsletter() {
		return notifyNewsletter;
	}

	public void setNotifyNewsletter(boolean notifyNewsletter) {
		this.notifyNewsletter = notifyNewsletter;
	}

	public boolean isNotifyMaintenance() {
		return notifyMaintenance;
	}

	public void setNotifyMaintenance(boolean notifyMaintenance) {
		this.notifyMaintenance = notifyMaintenance;
	}

	public boolean isNotifyActivities() {
		return notifyActivities;
	}

	public void setNotifyActivities(boolean notifyActivities) {
		this.notifyActivities = notifyActivities;
	}

	public String getDefaultLanguage() {
		return defaultLanguage;
	}

	public void setDefaultLanguage(String defaultLanguage) {
		this.defaultLanguage = defaultLanguage;
	}

	public UserRole getRole() {
		return role;
	}

	public void setRole(UserRole role) {
		this.role = role;
	}

	public String getOrganizationId() {
		return organizationId;
	}

	public void setOrganizationId(String organizationId) {
		this.organizationId = organizationId;
	}

	public String getConfirmationCode() {
		return confirmationCode;
	}

	public void setConfirmationCode(String confirmationCode) {
		this.confirmationCode = confirmationCode;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getInternationalPrefix() {
		return internationalPrefix;
	}

	public void setInternationalPrefix(String internationalPrefix) {
		this.internationalPrefix = internationalPrefix;
	}

}
