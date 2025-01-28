package rise.lib.business;

public class ForgetPasswordRequest extends RiseEntity {
	private String id;
	private String userId;
	private String confirmationCode;
	private double createdAt;
	private double expiresAt;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getConfirmationCode() {
		return confirmationCode;
	}

	public void setConfirmationCode(String confirmationCode) {
		this.confirmationCode = confirmationCode;
	}

	public double getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(double createdAt) {
		this.createdAt = createdAt;
	}

	public double getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(double expiresAt) {
		this.expiresAt = expiresAt;
	}

	

}
