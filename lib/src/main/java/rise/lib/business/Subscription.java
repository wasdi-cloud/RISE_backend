package rise.lib.business;

import java.util.ArrayList;

public class Subscription extends RiseEntity  {
	
	private String organizationId;
	
	private String name;
	
	private String type;
	
	private String description;
	
	private Double creationDate;
	
	private Double buyDate;
	
	private boolean valid;
	
	private Double expireDate;
	
	private ArrayList<String> plugins;
	
	private PaymentType paymentType;
	
	private Double price;
	
	private String currency;
	
	private boolean supportsArchive;
	
	private String id;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	

	public String getOrganizationId() {
		return organizationId;
	}

	public void setOrganizationId(String organizationId) {
		this.organizationId = organizationId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Double getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Double creationDate) {
		this.creationDate = creationDate;
	}

	public Double getBuyDate() {
		return buyDate;
	}

	public void setBuyDate(Double buyDate) {
		this.buyDate = buyDate;
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public Double getExpireDate() {
		return expireDate;
	}

	public void setExpireDate(Double expireDate) {
		this.expireDate = expireDate;
	}

	public ArrayList<String> getPlugins() {
		return plugins;
	}

	public void setPlugins(ArrayList<String> plugins) {
		this.plugins = plugins;
	}

	public PaymentType getPaymentType() {
		return paymentType;
	}

	public void setPaymentType(PaymentType paymentType) {
		this.paymentType = paymentType;
	}

	public Double getPrice() {
		return price;
	}

	public void setPrice(Double price) {
		this.price = price;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public boolean isSupportsArchive() {
		return supportsArchive;
	}

	public void setSupportsArchive(boolean supportsArchive) {
		this.supportsArchive = supportsArchive;
	}
	
	
	
}
