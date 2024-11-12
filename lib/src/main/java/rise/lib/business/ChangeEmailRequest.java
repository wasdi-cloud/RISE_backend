package rise.lib.business;

public class ChangeEmailRequest extends RiseEntity{
	private String id;
	private String oldEmail;
	private String newEmail;
	private String confirmationCode;
	/**
	 * @return the confirmationCode
	 */
	public String getConfirmationCode() {
		return confirmationCode;
	}
	/**
	 * @param confirmationCode the confirmationCode to set
	 */
	public void setConfirmationCode(String confirmationCode) {
		this.confirmationCode = confirmationCode;
	}
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}
	/**
	 * @return the oldEmail
	 */
	public String getOldEmail() {
		return oldEmail;
	}
	/**
	 * @param oldEmail the oldEmail to set
	 */
	public void setOldEmail(String oldEmail) {
		this.oldEmail = oldEmail;
	}
	/**
	 * @return the newEmail
	 */
	public String getNewEmail() {
		return newEmail;
	}
	/**
	 * @param newEmail the newEmail to set
	 */
	public void setNewEmail(String newEmail) {
		this.newEmail = newEmail;
	}
	
}