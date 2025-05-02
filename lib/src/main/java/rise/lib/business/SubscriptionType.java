package rise.lib.business;

public class SubscriptionType extends RiseEntity {
	
	private String description;
	
	private String stringCode;

	private String id;
	
	private int allowedAreas = 0;
	
	private double price = 0.0;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

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

	public int getAllowedAreas() {
		return allowedAreas;
	}

	public void setAllowedAreas(int allowedAreas) {
		this.allowedAreas = allowedAreas;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}
	
}
