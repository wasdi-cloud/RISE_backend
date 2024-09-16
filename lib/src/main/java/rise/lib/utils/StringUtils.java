package rise.lib.utils;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import rise.lib.utils.log.RiseLog;

/**
 * Utility class to handle String related operations.
 * 
 * @author PetruPetrescu
 *
 */
public final class StringUtils {

	private StringUtils() {
		throw new java.lang.UnsupportedOperationException("This is a utility class and cannot be instantiated");
	}
	
	/**
	 * Encode sUrl in URL Encoding
	 * @param sUrl String to encode
	 * @return Encoded String
	 */
	public static String encodeUrl(String sUrl) {
		try {
			return URLEncoder.encode(sUrl, java.nio.charset.StandardCharsets.UTF_8.toString());
		} catch (UnsupportedEncodingException oE) {
			RiseLog.debugLog("StringUtils.encodeUrl: could not encode URL due to " + oE + ".");
		}

		return sUrl;
	}

	public static String generateSha224(String input) {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-224");
			byte[] bytes = messageDigest.digest(input.getBytes());
			String digest = bytesToHexadecimal(bytes);

			return digest;
		} catch (NoSuchAlgorithmException oEx) {
			RiseLog.debugLog("StringUtils.generateSha224: invalid digest algorithm SHA-224: " + oEx + ".");
		} catch (Exception oEx) {
			RiseLog.debugLog("StringUtils.generateSha224: could not generate Sha224 due to " + oEx + ".");
		}

		return input;
	}
	
	/**
	 * Convert an array of bytes in a String representation
	 * @param bytes Array of bytes
	 * @return String hex representation 
	 */
	private static String bytesToHexadecimal(byte[] bytes) {
		// Convert byte array into signum representation
		BigInteger bigInteger = new BigInteger(1, bytes);

		// Convert message digest into hex value
		String hashtext = bigInteger.toString(16);

		// Add preceding 0s to make it 32 bit
		while (hashtext.length() < 32) {
			hashtext = "0" + hashtext;
		}

		// return the HashText
		return hashtext;
	}
	
	/**
	 * Assume that the sNumber parameter contains the representation of an integer.
	 * It returns the number incremented by 1.
	 * @param sNumber Input string with the number
	 * @return String with the number incremented, empty string otherwise
	 */
	public static String incrementIntegerString(String sNumber) {
		String sReturnString = "";
		
		try {
			int iNumber = Integer.parseInt(sNumber);
			
			iNumber = iNumber+1;
			sReturnString = "" + iNumber;
		}
		catch (Exception oEx) {
			RiseLog.errorLog("StringUtils.incrementIntegerString: not valid input string - ", oEx);
		}
		
		return sReturnString;
	}
	
	/**
	 * Assume that the sNumber parameter contains the representation of an integer.
	 * It returns the number decreased by 1.
	 * @param sNumber Input string with the number
	 * @return String with the number decreased, empty string otherwise
	 */
	public static String decrementIntegerString(String sNumber) {
		String sReturnString = "";
		
		try {
			int iNumber = Integer.parseInt(sNumber);
			
			iNumber = iNumber-1;
			sReturnString = "" + iNumber;
		}
		catch (Exception oEx) {
			RiseLog.errorLog("StringUtils.decrementIntegerString: not valid input string - ", oEx);
		}
		
		return sReturnString;
	}
	
	/**
	 * Check if a String is a valid integer
	 * In case of errors, will log and return 0
	 * @param sNumber Input string with the number
	 * @return Integer representation. NOTE that in case of errors returns 0 in any case
	 */
	public static boolean isValidInteger(String sNumber) {
		try {
			Integer.parseInt(sNumber);
			return true;
		}
		catch (Exception oEx) {
		}
		
		return false;
	}	
	
	/**
	 * Get an integer from a String.
	 * In case of errors, will log and return 0
	 * @param sNumber Input string with the number
	 * @return Integer representation. NOTE that in case of errors returns 0 in any case
	 */
	public static int getAsInteger(String sNumber) {
		try {
			int iReturn = Integer.parseInt(sNumber);
			return iReturn;
		}
		catch (Exception oEx) {
			RiseLog.errorLog("StringUtils.getAsInteger: not valid input string - ", oEx);
		}
		
		return 0;
	}

}
