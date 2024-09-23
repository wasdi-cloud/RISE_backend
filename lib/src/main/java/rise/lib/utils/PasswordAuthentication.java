package rise.lib.utils;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import rise.lib.config.RiseConfig;

/**
 * Hash passwords for storage, and test passwords against password tokens.
 * 
 * Instances of this class can be used concurrently by multiple threads.
 *  
 * @author erickson
 * @see <a href="http://stackoverflow.com/a/2861125/3474">StackOverflow</a>
 */
public final class PasswordAuthentication 
{
	/**
	   * Each token produced by this class uses this identifier as a prefix.
	   */
	  public static final String ID = "$513$";

	  /**
	   * The minimum recommended cost, used by default
	   */
	  public static final int DEFAULT_COST = 16;

	  private static final String ALGORITHM = "PBKDF2WithHmacSHA1";

	  private static final int SIZE = 128;

	  private static final Pattern layout = Pattern.compile("\\$513\\$(\\d\\d?)\\$(.{43})");

	  private final SecureRandom m_oSecureRandom;

	  private final int m_iCost;
	  
	  public PasswordAuthentication()
	  {
	    this(DEFAULT_COST);
	  }
	  
	  /**
	   * Create a password manager with a specified cost
	   * 
	   * @param cost the exponential computational cost of hashing a password, 0 to 30
	   */
	  public PasswordAuthentication(int cost)
	  {
	    iterations(cost); /* Validate cost */
	    this.m_iCost = cost;
	    this.m_oSecureRandom = new SecureRandom();
	  }

	  private static int iterations(int cost)
	  {
	    if ((cost < 0) || (cost > 30))
	      throw new IllegalArgumentException("cost: " + cost);
	    return 1 << cost;
	  }

	  /**
	   * Hash a password for storage.
	   * 
	   * @return a secure authentication token to be stored for later authentication 
	   */
	  public String hash(char[] password)
	  {
	    byte[] salt = new byte[SIZE / 8];
	    m_oSecureRandom.nextBytes(salt);
	    byte[] dk = pbkdf2(password, salt, 1 << m_iCost);
	    byte[] hash = new byte[salt.length + dk.length];
	    System.arraycopy(salt, 0, hash, 0, salt.length);
	    System.arraycopy(dk, 0, hash, salt.length, dk.length);
	    Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
	    return ID + m_iCost + '$' + enc.encodeToString(hash);
	  }

	  /**
	   * Authenticate with a password and a stored password token.
	   * 
	   * @return true if the password and token match
	   */
	  public boolean authenticate(char[] password, String token)
	  {
	    Matcher m = layout.matcher(token);
	    if (!m.matches())
	      throw new IllegalArgumentException("Invalid token format");
	    int iterations = iterations(Integer.parseInt(m.group(1)));
	    byte[] hash = Base64.getUrlDecoder().decode(m.group(2));
	    byte[] salt = Arrays.copyOfRange(hash, 0, SIZE / 8);
	    byte[] check = pbkdf2(password, salt, iterations);
	    int zero = 0;
	    for (int idx = 0; idx < check.length; ++idx)
	      zero |= hash[salt.length + idx] ^ check[idx];
	    return zero == 0;
	  }
	  
	  public boolean authenticate(String sPassword, String token) {
		  return authenticate(sPassword.toCharArray(), token);
	  }
	  
	  

	  private static byte[] pbkdf2(char[] password, byte[] salt, int iterations)
	  {
	    KeySpec spec = new PBEKeySpec(password, salt, iterations, SIZE);
	    try {
	      SecretKeyFactory f = SecretKeyFactory.getInstance(ALGORITHM);
	      return f.generateSecret(spec).getEncoded();
	    }
	    catch (NoSuchAlgorithmException ex) {
	      throw new IllegalStateException("Missing algorithm: " + ALGORITHM, ex);
	    }
	    catch (InvalidKeySpecException ex) {
	      throw new IllegalStateException("Invalid SecretKeyFactory", ex);
	    }
	  }
	  
	  public boolean isValidPassword(String sPassword) {
		  
		  if (Utils.isNullOrEmpty(sPassword)) return false;
		  if (sPassword.length()<RiseConfig.Current.security.minPwLenght) return false;
		  
		  if (RiseConfig.Current.security.mustContainNumber) {
			  if (!sPassword.matches(".*\\d.*")) return false;
		  }
		  
		  if (RiseConfig.Current.security.mustContainSymbol) {
			  Pattern oPattern = Pattern.compile ("[!@#$%&*()_+=|<>?{}\\[\\]~-]");
			  Matcher oHasDigit = oPattern.matcher(sPassword);
			  boolean bFound = oHasDigit.find();

			  if (!bFound) return false;			  
		  }

		  if (RiseConfig.Current.security.mustMixUpperLowerCase) {
			  Pattern oLower = Pattern.compile("[a-z]");
			  Pattern oUpper = Pattern.compile("[A-Z]");

			  Matcher oHasLower = oLower.matcher(sPassword);
		      Matcher oHasUpper= oUpper.matcher(sPassword);
		      
		      if (! (oHasLower.find() && oHasUpper.find() )) return false;
		  }
		  
		  return true;
	  }
	  
	  /**
	   * Check if it is a valid User Id
	   * @param sUserId
	   * @return
	   */
	  public boolean isValidUserId(String sUserId) {
		  if (Utils.isNullOrEmpty(sUserId)) return false;
		  
		  if (sUserId.length()<RiseConfig.Current.security.minUserIdLenght) return false;
		  
		  return true;
	  }

}
