package rise.lib.business;

public class Session extends RiseEntity {
	
    /**
     * Unique session ID
     */
	private String token;
	
	/**
	 * User ID
	 */
    private String userId;
    
    /**
     * Login Date
     */
    private Double loginDate;
    
    /**
     * Last activity timestamp
     */
    private Double lastTouch;

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public Double getLoginDate() {
		return loginDate;
	}

	public void setLoginDate(Double loginDate) {
		this.loginDate = loginDate;
	}

	public Double getLastTouch() {
		return lastTouch;
	}

	public void setLastTouch(Double lastTouch) {
		this.lastTouch = lastTouch;
	}
	
}
