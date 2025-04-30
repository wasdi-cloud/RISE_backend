package rise.lib.business;

public class Organization extends RiseEntity {
	
	private String name;
	
	private String type;
	
	private String phone;
	
	private String country;
	
	private String city;
	
	private String street;
	
	private String number;
	
	private String internationalPrefix;
	
	private String postalCode;
	
	private String vat;
	
	private Double creationDate;
	
	private String id;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}


	public Double getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Double creationDate) {
		this.creationDate = creationDate;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getVat() {
		return vat;
	}

	public void setVat(String vat) {
		this.vat = vat;
	}

	public String getInternationalPrefix() {
		return internationalPrefix;
	}

	public void setInternationalPrefix(String internationalPrefix) {
		this.internationalPrefix = internationalPrefix;
	}
		
}
