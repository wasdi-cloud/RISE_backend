package rise.lib.config;

public class SecurityConfig {
	
	public double maxConfirmationAgeSeconds = 24*60*60;
	
	//this should be three month(as required)
	//for the test env we are making it to 6 months 
	public double maxPasswordAgeSeconds = 3*30*24*60*60*2;
	
	public int minPwLenght = 8;
	
	public int minUserIdLenght = 8;
	
	public boolean mustMixUpperLowerCase = true;
	
	public boolean mustContainNumber = true;
	
	public boolean mustContainSymbol = true;
	
	public double maxOTPAgeSeconds = 60*30;
	
	public String inviteConfirmAddress = "https://risetest.wasdi.net/user/confirm";
	
	public String changeEmailConfirm="https://risetest.wasdi.net/user/confirm-change-email";
	
	public String registerConfirmAddress = "rise.wasdi.net/user/confirm";
}
