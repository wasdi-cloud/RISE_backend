package rise.lib.business;

public class PasswordChangeRequest {
	private String otpId;
	private String userId;
	private String password;
	/**
	 * @return the otpId
	 */
	public String getOtpId() {
		return otpId;
	}
	/**
	 * @param otpId the otpId to set
	 */
	public void setOtpId(String otpId) {
		this.otpId = otpId;
	}
	/**
	 * @return the userId
	 */
	public String getUserId() {
		return userId;
	}
	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}
	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}
	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}
	
	
}
