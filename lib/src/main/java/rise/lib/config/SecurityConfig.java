package rise.lib.config;

public class SecurityConfig {
	
	public double maxConfirmationAgeSeconds = 24*60*60;
	
	public double maxPasswordAgeSeconds = 3*30*24*60*60;
	
	public int minPwLenght = 8;
	
	public boolean mustMixUpperLowerCase = true;
	
	public boolean mustContainNumber = true;
	
	public boolean mustContainSymbol = true;
	
	public double maxOTPAgeSeconds = 60*30;
}
