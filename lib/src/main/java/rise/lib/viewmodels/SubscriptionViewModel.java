package rise.lib.viewmodels;

import rise.lib.business.PaymentType;

public class SubscriptionViewModel {
	public String id;
	
	public String organizationId;
	
	public String name;
	
	public String type;
	
	public String description;
	
	public Double creationDate;
	
	public Double buyDate;
	
	public boolean valid;
	
	public Double expireDate;
	
	public PaymentType paymentType;
	
	public Double price;
	
	public String currency;
	
	public boolean supportsArchive;
}
