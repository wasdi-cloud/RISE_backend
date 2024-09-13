package rise.lib.business;

import rise.lib.utils.Utils;

public enum PaymentType {
	MONTH("MONTH"),
	YEAR("YEAR");
	
	private final String value;
	
	PaymentType(String sValue) {
		this.value = sValue;
	}

	public String getString() {
		return value;
	}		
	
	public static boolean isValid(String sValue) {
		if (Utils.isNullOrEmpty(sValue)) return false;
		
		if (sValue.equals(MONTH.getString())) return true;
		if (sValue.equals(YEAR.getString())) return true;
		
		return false;
	}
}
