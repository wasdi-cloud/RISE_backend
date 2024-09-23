package rise.lib.viewmodels;

import java.util.ArrayList;

import rise.lib.utils.Utils;

public class ErrorViewModel extends RiseViewModel {
	
	public ErrorViewModel() {
		
	}

	public ErrorViewModel(ArrayList<String> asStringCodes) {
		if (asStringCodes!=null) {
			errorStringCodes.addAll(asStringCodes);
		}
	}
	
	public ErrorViewModel(ArrayList<String> asStringCodes, int iHttpCode) {
		if (asStringCodes!=null) {
			errorStringCodes.addAll(asStringCodes);
		}
		
		httpCode = iHttpCode;
	}	

	public ErrorViewModel(String sStringCode) {
		if (!Utils.isNullOrEmpty(sStringCode)) {
			errorStringCodes.add(sStringCode);
		}
	}
	
	public ErrorViewModel(String sStringCode, int iHttpCode) {
		if (!Utils.isNullOrEmpty(sStringCode)) {
			errorStringCodes.add(sStringCode);
		}
		httpCode = iHttpCode;
	}
		
	public int httpCode;
	
	public ArrayList<String> errorStringCodes = new ArrayList<>();
}
