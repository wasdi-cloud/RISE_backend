package rise.lib.business;

public class SubscriptionType extends RiseEntity {
	
	private String description;
	
	private String stringCode;

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getStringCode() {
		return stringCode;
	}

	public void setStringCode(String stringCode) {
		this.stringCode = stringCode;
	}
	
}
